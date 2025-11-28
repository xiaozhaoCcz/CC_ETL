# 远程控制系统 C# 伪代码实现

## 目录结构

```
RemoteControlSystem/
├── Common/                    # 公共模块
│   ├── Protocol/             # 协议定义
│   └── Security/             # 加密安全
├── Agent/                     # 被控端
│   ├── ScreenCapture/        # 屏幕捕获
│   ├── VideoEncoder/         # 视频编码
│   ├── InputSimulator/       # 输入模拟
│   └── NetworkClient/        # 网络客户端
├── Controller/                # 控制端
│   ├── VideoDecoder/         # 视频解码
│   ├── InputCapture/         # 输入捕获
│   └── NetworkClient/        # 网络客户端
└── RelayServer/              # 中继服务器
    ├── SignalingServer/      # 信令服务
    └── TurnServer/           # 流量中继
```

---

## 1. 协议定义（Common/Protocol）

```csharp
// 消息类型枚举
public enum MessageType : byte
{
    // 控制消息
    Handshake = 0x01,
    Heartbeat = 0x02,
    Authentication = 0x03,
    
    // 屏幕数据
    VideoFrame = 0x10,
    VideoFrameDelta = 0x11,  // 增量帧
    
    // 输入控制
    MouseMove = 0x20,
    MouseClick = 0x21,
    KeyPress = 0x22,
    
    // 其他
    Clipboard = 0x30,
    FileTransfer = 0x31
}

// 基础消息包
[StructLayout(LayoutKind.Sequential, Pack = 1)]
public struct PacketHeader
{
    public MessageType Type;
    public uint SequenceNumber;    // 序列号
    public uint Timestamp;         // 时间戳（毫秒）
    public ushort PayloadLength;   // 负载长度
    public byte Flags;             // 标志位
    public byte Reserved;
}

// 视频帧数据包
public class VideoFramePacket
{
    public PacketHeader Header;
    public int Width;
    public int Height;
    public VideoCodec Codec;       // H264, H265, VP8
    public byte[] Data;            // 编码后的数据
    public Rectangle[] DirtyRects; // 变化区域
}

// 鼠标事件包
public class MouseEventPacket
{
    public PacketHeader Header;
    public int X;
    public int Y;
    public MouseButton Button;     // Left, Right, Middle
    public MouseAction Action;     // Move, Down, Up, Wheel
    public int WheelDelta;
}

// 键盘事件包
public class KeyboardEventPacket
{
    public PacketHeader Header;
    public ushort VirtualKeyCode;
    public ushort ScanCode;
    public KeyAction Action;       // Down, Up
    public KeyModifiers Modifiers; // Ctrl, Alt, Shift
}

// 协议序列化器
public class ProtocolSerializer
{
    public static byte[] Serialize<T>(T packet) where T : struct
    {
        using var stream = new MemoryStream();
        using var writer = new BinaryWriter(stream);
        
        // 写入头部
        // 写入数据
        // 计算校验和
        
        return stream.ToArray();
    }
    
    public static T Deserialize<T>(byte[] data) where T : struct
    {
        using var stream = new MemoryStream(data);
        using var reader = new BinaryReader(stream);
        
        // 读取并解析
        return default(T);
    }
}
```

---

## 2. 屏幕捕获模块（Agent/ScreenCapture）

```csharp
using SharpDX.DXGI;
using SharpDX.Direct3D11;

// 屏幕捕获器接口
public interface IScreenCapturer
{
    event Action<CaptureFrame> OnFrameCaptured;
    void Start();
    void Stop();
    void SetFrameRate(int fps);
}

// Desktop Duplication API 实现
public class DxgiScreenCapturer : IScreenCapturer
{
    private OutputDuplication _duplication;
    private Device _device;
    private Texture2D _stagingTexture;
    private Thread _captureThread;
    private volatile bool _isRunning;
    private int _targetFps = 60;
    
    public event Action<CaptureFrame> OnFrameCaptured;
    
    public void Initialize(int adapterIndex, int outputIndex)
    {
        // 1. 创建D3D11设备
        var adapter = new Factory1().GetAdapter1(adapterIndex);
        _device = new Device(adapter);
        
        // 2. 获取输出接口
        var output = adapter.GetOutput(outputIndex);
        var output1 = output.QueryInterface<Output1>();
        
        // 3. 创建Desktop Duplication
        _duplication = output1.DuplicateOutput(_device);
        
        // 4. 创建Staging纹理（用于CPU读取）
        var desc = new Texture2DDescription
        {
            Width = output.Description.DesktopBounds.Right,
            Height = output.Description.DesktopBounds.Bottom,
            MipLevels = 1,
            ArraySize = 1,
            Format = Format.B8G8R8A8_UNorm,
            SampleDescription = new SampleDescription(1, 0),
            Usage = ResourceUsage.Staging,
            CpuAccessFlags = CpuAccessFlags.Read,
            BindFlags = BindFlags.None
        };
        _stagingTexture = new Texture2D(_device, desc);
    }
    
    public void Start()
    {
        _isRunning = true;
        _captureThread = new Thread(CaptureLoop);
        _captureThread.Priority = ThreadPriority.Highest;
        _captureThread.Start();
    }
    
    public void Stop()
    {
        _isRunning = false;
        _captureThread?.Join();
    }
    
    private void CaptureLoop()
    {
        int frameInterval = 1000 / _targetFps;
        var stopwatch = Stopwatch.StartNew();
        
        while (_isRunning)
        {
            long frameStart = stopwatch.ElapsedMilliseconds;
            
            try
            {
                // 1. 尝试获取帧
                var result = _duplication.TryAcquireNextFrame(
                    timeout: 1000,
                    out var frameInfo,
                    out var desktopResource
                );
                
                if (result.Success && frameInfo.TotalMetadataBufferSize > 0)
                {
                    // 2. 获取变化区域（Dirty Rects）
                    var dirtyRects = new Rectangle[frameInfo.TotalMetadataBufferSize / 
                                                   Marshal.SizeOf<Rectangle>()];
                    _duplication.GetFrameDirtyRects(dirtyRects);
                    
                    // 3. 复制纹理到Staging
                    using var texture = desktopResource.QueryInterface<Texture2D>();
                    _device.ImmediateContext.CopyResource(texture, _stagingTexture);
                    
                    // 4. 读取像素数据
                    var dataBox = _device.ImmediateContext.MapSubresource(
                        _stagingTexture, 0, MapMode.Read, MapFlags.None
                    );
                    
                    var frame = new CaptureFrame
                    {
                        Width = _stagingTexture.Description.Width,
                        Height = _stagingTexture.Description.Height,
                        Stride = dataBox.RowPitch,
                        Data = new byte[dataBox.SlicePitch],
                        DirtyRects = dirtyRects,
                        Timestamp = stopwatch.ElapsedMilliseconds
                    };
                    
                    // 5. 复制数据
                    Marshal.Copy(dataBox.DataPointer, frame.Data, 0, frame.Data.Length);
                    
                    _device.ImmediateContext.UnmapSubresource(_stagingTexture, 0);
                    
                    // 6. 释放帧
                    _duplication.ReleaseFrame();
                    
                    // 7. 触发事件
                    OnFrameCaptured?.Invoke(frame);
                }
            }
            catch (SharpDXException ex)
            {
                // 处理错误（如屏幕分辨率改变）
                if (ex.ResultCode == ResultCode.AccessLost)
                {
                    ReInitializeDuplication();
                }
            }
            
            // 8. 帧率控制
            long elapsed = stopwatch.ElapsedMilliseconds - frameStart;
            int sleepTime = (int)(frameInterval - elapsed);
            if (sleepTime > 0)
            {
                Thread.Sleep(sleepTime);
            }
        }
    }
    
    private void ReInitializeDuplication()
    {
        _duplication?.Dispose();
        // 重新初始化...
    }
}

// 捕获帧数据结构
public class CaptureFrame
{
    public int Width { get; set; }
    public int Height { get; set; }
    public int Stride { get; set; }
    public byte[] Data { get; set; }
    public Rectangle[] DirtyRects { get; set; }
    public long Timestamp { get; set; }
}
```

---

## 3. 视频编码模块（Agent/VideoEncoder）

```csharp
using FFmpeg.AutoGen;

// 编码器接口
public interface IVideoEncoder
{
    void Initialize(VideoEncoderConfig config);
    byte[] Encode(CaptureFrame frame, out bool isKeyFrame);
    void SetBitrate(int bitrate);
    void Dispose();
}

// 编码器配置
public class VideoEncoderConfig
{
    public int Width { get; set; }
    public int Height { get; set; }
    public int Framerate { get; set; } = 60;
    public int Bitrate { get; set; } = 2000000; // 2Mbps
    public VideoCodec Codec { get; set; } = VideoCodec.H264;
    public bool UseHardwareAcceleration { get; set; } = true;
    public string Preset { get; set; } = "ultrafast"; // zerolatency
}

// FFmpeg H.264 编码器
public unsafe class FFmpegH264Encoder : IVideoEncoder
{
    private AVCodecContext* _codecContext;
    private AVFrame* _frame;
    private AVPacket* _packet;
    private SwsContext* _swsContext;
    private int _frameCounter;
    
    public void Initialize(VideoEncoderConfig config)
    {
        ffmpeg.avcodec_register_all();
        
        // 1. 查找编码器（优先硬件）
        AVCodec* codec;
        if (config.UseHardwareAcceleration)
        {
            // NVENC (NVIDIA)
            codec = ffmpeg.avcodec_find_encoder_by_name("h264_nvenc");
            if (codec == null)
            {
                // QSV (Intel)
                codec = ffmpeg.avcodec_find_encoder_by_name("h264_qsv");
            }
            if (codec == null)
            {
                // AMF (AMD)
                codec = ffmpeg.avcodec_find_encoder_by_name("h264_amf");
            }
        }
        
        // 降级到软编码
        if (codec == null)
        {
            codec = ffmpeg.avcodec_find_encoder(AVCodecID.AV_CODEC_ID_H264);
        }
        
        // 2. 创建编码器上下文
        _codecContext = ffmpeg.avcodec_alloc_context3(codec);
        _codecContext->width = config.Width;
        _codecContext->height = config.Height;
        _codecContext->time_base = new AVRational { num = 1, den = config.Framerate };
        _codecContext->framerate = new AVRational { num = config.Framerate, den = 1 };
        _codecContext->bit_rate = config.Bitrate;
        _codecContext->gop_size = config.Framerate; // 1秒一个关键帧
        _codecContext->max_b_frames = 0; // 低延迟：禁用B帧
        _codecContext->pix_fmt = AVPixelFormat.AV_PIX_FMT_YUV420P;
        
        // 3. 设置编码参数（超低延迟）
        ffmpeg.av_opt_set(_codecContext->priv_data, "preset", "ultrafast", 0);
        ffmpeg.av_opt_set(_codecContext->priv_data, "tune", "zerolatency", 0);
        ffmpeg.av_opt_set(_codecContext->priv_data, "crf", "23", 0);
        
        // 硬件编码器特殊设置
        if (config.UseHardwareAcceleration)
        {
            ffmpeg.av_opt_set(_codecContext->priv_data, "delay", "0", 0);
            ffmpeg.av_opt_set(_codecContext->priv_data, "zerolatency", "1", 0);
        }
        
        // 4. 打开编码器
        int ret = ffmpeg.avcodec_open2(_codecContext, codec, null);
        if (ret < 0)
        {
            throw new Exception($"Failed to open codec: {ret}");
        }
        
        // 5. 分配帧和包
        _frame = ffmpeg.av_frame_alloc();
        _frame->format = (int)_codecContext->pix_fmt;
        _frame->width = config.Width;
        _frame->height = config.Height;
        ffmpeg.av_frame_get_buffer(_frame, 32);
        
        _packet = ffmpeg.av_packet_alloc();
        
        // 6. 创建像素格式转换器（BGRA -> YUV420P）
        _swsContext = ffmpeg.sws_getContext(
            config.Width, config.Height, AVPixelFormat.AV_PIX_FMT_BGRA,
            config.Width, config.Height, AVPixelFormat.AV_PIX_FMT_YUV420P,
            ffmpeg.SWS_FAST_BILINEAR, null, null, null
        );
    }
    
    public byte[] Encode(CaptureFrame frame, out bool isKeyFrame)
    {
        isKeyFrame = false;
        
        // 1. 转换像素格式 BGRA -> YUV420P
        fixed (byte* srcPtr = frame.Data)
        {
            byte*[] srcData = new byte*[] { srcPtr, null, null, null };
            int[] srcLinesize = new int[] { frame.Stride, 0, 0, 0 };
            
            ffmpeg.sws_scale(
                _swsContext,
                srcData, srcLinesize, 0, frame.Height,
                _frame->data, _frame->linesize
            );
        }
        
        // 2. 设置帧参数
        _frame->pts = _frameCounter++;
        
        // 3. 发送帧到编码器
        int ret = ffmpeg.avcodec_send_frame(_codecContext, _frame);
        if (ret < 0)
        {
            throw new Exception($"Error sending frame: {ret}");
        }
        
        // 4. 接收编码后的包
        ret = ffmpeg.avcodec_receive_packet(_codecContext, _packet);
        if (ret == ffmpeg.AVERROR(ffmpeg.EAGAIN) || ret == ffmpeg.AVERROR_EOF)
        {
            return null; // 需要更多帧
        }
        else if (ret < 0)
        {
            throw new Exception($"Error receiving packet: {ret}");
        }
        
        // 5. 复制数据
        byte[] encodedData = new byte[_packet->size];
        Marshal.Copy((IntPtr)_packet->data, encodedData, 0, _packet->size);
        
        // 6. 检查是否为关键帧
        isKeyFrame = (_packet->flags & ffmpeg.AV_PKT_FLAG_KEY) != 0;
        
        // 7. 释放包
        ffmpeg.av_packet_unref(_packet);
        
        return encodedData;
    }
    
    public void SetBitrate(int bitrate)
    {
        _codecContext->bit_rate = bitrate;
    }
    
    public void Dispose()
    {
        if (_frame != null)
        {
            ffmpeg.av_frame_free(&_frame);
        }
        if (_packet != null)
        {
            ffmpeg.av_packet_free(&_packet);
        }
        if (_codecContext != null)
        {
            ffmpeg.avcodec_free_context(&_codecContext);
        }
        if (_swsContext != null)
        {
            ffmpeg.sws_freeContext(_swsContext);
        }
    }
}
```

---

## 4. 网络传输层（DotNetty实现）

```csharp
using DotNetty.Transport.Channels;
using DotNetty.Transport.Bootstrapping;
using DotNetty.Codecs;

// 网络客户端（被控端和控制端通用）
public class RemoteControlClient
{
    private IChannel _channel;
    private Bootstrap _bootstrap;
    private IEventLoopGroup _group;
    
    public event Action<VideoFramePacket> OnVideoFrameReceived;
    public event Action<MouseEventPacket> OnMouseEventReceived;
    public event Action<KeyboardEventPacket> OnKeyboardEventReceived;
    public event Action OnConnected;
    public event Action OnDisconnected;
    
    public async Task ConnectAsync(string host, int port)
    {
        _group = new MultithreadEventLoopGroup();
        
        _bootstrap = new Bootstrap()
            .Group(_group)
            .Channel<TcpSocketChannel>()
            .Option(ChannelOption.TcpNodelay, true)        // 禁用Nagle算法
            .Option(ChannelOption.SoKeepalive, true)       // 启用心跳
            .Option(ChannelOption.ConnectTimeout, TimeSpan.FromSeconds(10))
            .Handler(new ActionChannelInitializer<ISocketChannel>(channel =>
            {
                var pipeline = channel.Pipeline;
                
                // 1. 添加编解码器
                pipeline.AddLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                    maxFrameLength: 10 * 1024 * 1024,  // 10MB
                    lengthFieldOffset: 0,
                    lengthFieldLength: 4,
                    lengthAdjustment: 0,
                    initialBytesToStrip: 4
                ));
                
                pipeline.AddLast("frameEncoder", new LengthFieldPrepender(4));
                
                // 2. 添加业务处理器
                pipeline.AddLast("handler", new RemoteControlClientHandler(this));
            }));
        
        _channel = await _bootstrap.ConnectAsync(host, port);
        OnConnected?.Invoke();
    }
    
    public async Task DisconnectAsync()
    {
        if (_channel != null)
        {
            await _channel.CloseAsync();
            await _group.ShutdownGracefullyAsync();
        }
        OnDisconnected?.Invoke();
    }
    
    public async Task SendVideoFrameAsync(VideoFramePacket packet)
    {
        var data = SerializePacket(packet);
        await _channel.WriteAndFlushAsync(Unpooled.WrappedBuffer(data));
    }
    
    public async Task SendMouseEventAsync(MouseEventPacket packet)
    {
        var data = SerializePacket(packet);
        await _channel.WriteAndFlushAsync(Unpooled.WrappedBuffer(data));
    }
    
    public async Task SendKeyboardEventAsync(KeyboardEventPacket packet)
    {
        var data = SerializePacket(packet);
        await _channel.WriteAndFlushAsync(Unpooled.WrappedBuffer(data));
    }
    
    private byte[] SerializePacket<T>(T packet)
    {
        // 使用ProtoBuf或自定义序列化
        return ProtocolSerializer.Serialize(packet);
    }
    
    internal void HandleMessage(byte[] data)
    {
        // 根据消息类型分发
        var header = ParseHeader(data);
        
        switch (header.Type)
        {
            case MessageType.VideoFrame:
                var videoPacket = ProtocolSerializer.Deserialize<VideoFramePacket>(data);
                OnVideoFrameReceived?.Invoke(videoPacket);
                break;
                
            case MessageType.MouseMove:
            case MessageType.MouseClick:
                var mousePacket = ProtocolSerializer.Deserialize<MouseEventPacket>(data);
                OnMouseEventReceived?.Invoke(mousePacket);
                break;
                
            case MessageType.KeyPress:
                var keyPacket = ProtocolSerializer.Deserialize<KeyboardEventPacket>(data);
                OnKeyboardEventReceived?.Invoke(keyPacket);
                break;
        }
    }
    
    private PacketHeader ParseHeader(byte[] data)
    {
        // 解析包头
        return new PacketHeader();
    }
}

// DotNetty 处理器
public class RemoteControlClientHandler : SimpleChannelInboundHandler<IByteBuffer>
{
    private readonly RemoteControlClient _client;
    
    public RemoteControlClientHandler(RemoteControlClient client)
    {
        _client = client;
    }
    
    protected override void ChannelRead0(IChannelHandlerContext ctx, IByteBuffer msg)
    {
        // 读取数据
        byte[] data = new byte[msg.ReadableBytes];
        msg.ReadBytes(data);
        
        // 交给客户端处理
        _client.HandleMessage(data);
    }
    
    public override void ChannelActive(IChannelHandlerContext context)
    {
        Console.WriteLine("Channel connected");
        base.ChannelActive(context);
    }
    
    public override void ChannelInactive(IChannelHandlerContext context)
    {
        Console.WriteLine("Channel disconnected");
        base.ChannelInactive(context);
    }
    
    public override void ExceptionCaught(IChannelHandlerContext context, Exception exception)
    {
        Console.WriteLine($"Exception: {exception}");
        context.CloseAsync();
    }
}

// UDP传输（用于视频流）
public class UdpVideoTransport
{
    private Socket _socket;
    private EndPoint _remoteEndpoint;
    private Thread _receiveThread;
    
    public event Action<byte[]> OnDataReceived;
    
    public void Initialize(int localPort)
    {
        _socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
        _socket.Bind(new IPEndPoint(IPAddress.Any, localPort));
        
        // 设置缓冲区大小
        _socket.SendBufferSize = 2 * 1024 * 1024;    // 2MB
        _socket.ReceiveBufferSize = 2 * 1024 * 1024;
    }
    
    public void StartReceiving()
    {
        _receiveThread = new Thread(ReceiveLoop);
        _receiveThread.Start();
    }
    
    private void ReceiveLoop()
    {
        byte[] buffer = new byte[65536];
        EndPoint remoteEP = new IPEndPoint(IPAddress.Any, 0);
        
        while (true)
        {
            try
            {
                int received = _socket.ReceiveFrom(buffer, ref remoteEP);
                byte[] data = new byte[received];
                Array.Copy(buffer, data, received);
                
                OnDataReceived?.Invoke(data);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"UDP receive error: {ex}");
            }
        }
    }
    
    public void Send(byte[] data, string host, int port)
    {
        _remoteEndpoint = new IPEndPoint(IPAddress.Parse(host), port);
        _socket.SendTo(data, _remoteEndpoint);
    }
}
```

---

## 5. 输入模拟模块（Agent/InputSimulator）

```csharp
using System.Runtime.InteropServices;

// Windows API 声明
public static class NativeMethods
{
    [DllImport("user32.dll")]
    public static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);
    
    [DllImport("user32.dll")]
    public static extern short GetAsyncKeyState(int vKey);
    
    [StructLayout(LayoutKind.Sequential)]
    public struct INPUT
    {
        public InputType Type;
        public InputUnion Union;
    }
    
    [StructLayout(LayoutKind.Explicit)]
    public struct InputUnion
    {
        [FieldOffset(0)]
        public MOUSEINPUT Mouse;
        
        [FieldOffset(0)]
        public KEYBDINPUT Keyboard;
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public MouseEventFlags dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
    
    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public KeyEventFlags dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
    
    [Flags]
    public enum MouseEventFlags : uint
    {
        Move = 0x0001,
        LeftDown = 0x0002,
        LeftUp = 0x0004,
        RightDown = 0x0008,
        RightUp = 0x0010,
        MiddleDown = 0x0020,
        MiddleUp = 0x0040,
        Wheel = 0x0800,
        Absolute = 0x8000
    }
    
    [Flags]
    public enum KeyEventFlags : uint
    {
        KeyDown = 0x0000,
        ExtendedKey = 0x0001,
        KeyUp = 0x0002,
        Unicode = 0x0004,
        ScanCode = 0x0008
    }
    
    public enum InputType : uint
    {
        Mouse = 0,
        Keyboard = 1
    }
}

// 输入模拟器
public class InputSimulator
{
    private int _screenWidth;
    private int _screenHeight;
    
    public InputSimulator()
    {
        _screenWidth = Screen.PrimaryScreen.Bounds.Width;
        _screenHeight = Screen.PrimaryScreen.Bounds.Height;
    }
    
    // 模拟鼠标移动
    public void SimulateMouseMove(int x, int y)
    {
        // 转换为绝对坐标 (0-65535)
        int absoluteX = (x * 65536) / _screenWidth;
        int absoluteY = (y * 65536) / _screenHeight;
        
        var input = new NativeMethods.INPUT
        {
            Type = NativeMethods.InputType.Mouse,
            Union = new NativeMethods.InputUnion
            {
                Mouse = new NativeMethods.MOUSEINPUT
                {
                    dx = absoluteX,
                    dy = absoluteY,
                    mouseData = 0,
                    dwFlags = NativeMethods.MouseEventFlags.Move | 
                             NativeMethods.MouseEventFlags.Absolute,
                    time = 0,
                    dwExtraInfo = IntPtr.Zero
                }
            }
        };
        
        NativeMethods.SendInput(1, new[] { input }, Marshal.SizeOf<NativeMethods.INPUT>());
    }
    
    // 模拟鼠标点击
    public void SimulateMouseClick(MouseButton button, bool isDown)
    {
        MouseEventFlags flag = button switch
        {
            MouseButton.Left => isDown ? MouseEventFlags.LeftDown : MouseEventFlags.LeftUp,
            MouseButton.Right => isDown ? MouseEventFlags.RightDown : MouseEventFlags.RightUp,
            MouseButton.Middle => isDown ? MouseEventFlags.MiddleDown : MouseEventFlags.MiddleUp,
            _ => MouseEventFlags.Move
        };
        
        var input = new NativeMethods.INPUT
        {
            Type = NativeMethods.InputType.Mouse,
            Union = new NativeMethods.InputUnion
            {
                Mouse = new NativeMethods.MOUSEINPUT
                {
                    dx = 0,
                    dy = 0,
                    mouseData = 0,
                    dwFlags = flag,
                    time = 0,
                    dwExtraInfo = IntPtr.Zero
                }
            }
        };
        
        NativeMethods.SendInput(1, new[] { input }, Marshal.SizeOf<NativeMethods.INPUT>());
    }
    
    // 模拟鼠标滚轮
    public void SimulateMouseWheel(int delta)
    {
        var input = new NativeMethods.INPUT
        {
            Type = NativeMethods.InputType.Mouse,
            Union = new NativeMethods.InputUnion
            {
                Mouse = new NativeMethods.MOUSEINPUT
                {
                    dx = 0,
                    dy = 0,
                    mouseData = (uint)delta,
                    dwFlags = NativeMethods.MouseEventFlags.Wheel,
                    time = 0,
                    dwExtraInfo = IntPtr.Zero
                }
            }
        };
        
        NativeMethods.SendInput(1, new[] { input }, Marshal.SizeOf<NativeMethods.INPUT>());
    }
    
    // 模拟键盘按键
    public void SimulateKeyPress(ushort virtualKeyCode, bool isDown)
    {
        var input = new NativeMethods.INPUT
        {
            Type = NativeMethods.InputType.Keyboard,
            Union = new NativeMethods.InputUnion
            {
                Keyboard = new NativeMethods.KEYBDINPUT
                {
                    wVk = virtualKeyCode,
                    wScan = 0,
                    dwFlags = isDown ? KeyEventFlags.KeyDown : KeyEventFlags.KeyUp,
                    time = 0,
                    dwExtraInfo = IntPtr.Zero
                }
            }
        };
        
        NativeMethods.SendInput(1, new[] { input }, Marshal.SizeOf<NativeMethods.INPUT>());
    }
    
    // 模拟组合键（Ctrl+C等）
    public void SimulateHotkey(params ushort[] virtualKeyCodes)
    {
        var inputs = new List<NativeMethods.INPUT>();
        
        // 按下所有键
        foreach (var vk in virtualKeyCodes)
        {
            inputs.Add(new NativeMethods.INPUT
            {
                Type = NativeMethods.InputType.Keyboard,
                Union = new NativeMethods.InputUnion
                {
                    Keyboard = new NativeMethods.KEYBDINPUT
                    {
                        wVk = vk,
                        dwFlags = KeyEventFlags.KeyDown
                    }
                }
            });
        }
        
        // 释放所有键（逆序）
        foreach (var vk in virtualKeyCodes.Reverse())
        {
            inputs.Add(new NativeMethods.INPUT
            {
                Type = NativeMethods.InputType.Keyboard,
                Union = new NativeMethods.InputUnion
                {
                    Keyboard = new NativeMethods.KEYBDINPUT
                    {
                        wVk = vk,
                        dwFlags = KeyEventFlags.KeyUp
                    }
                }
            });
        }
        
        NativeMethods.SendInput((uint)inputs.Count, inputs.ToArray(), 
                               Marshal.SizeOf<NativeMethods.INPUT>());
    }
}
```

---

## 6. 被控端主程序（Agent）

```csharp
public class RemoteControlAgent
{
    private DxgiScreenCapturer _screenCapturer;
    private FFmpegH264Encoder _videoEncoder;
    private RemoteControlClient _networkClient;
    private InputSimulator _inputSimulator;
    
    private NetworkQualityMonitor _qualityMonitor;
    private BitrateController _bitrateController;
    
    private string _relayServerHost = "relay.example.com";
    private int _relayServerPort = 8888;
    
    public async Task StartAsync()
    {
        // 1. 初始化屏幕捕获
        _screenCapturer = new DxgiScreenCapturer();
        _screenCapturer.Initialize(adapterIndex: 0, outputIndex: 0);
        _screenCapturer.OnFrameCaptured += OnFrameCaptured;
        
        // 2. 初始化视频编码器
        _videoEncoder = new FFmpegH264Encoder();
        var encoderConfig = new VideoEncoderConfig
        {
            Width = 1920,
            Height = 1080,
            Framerate = 60,
            Bitrate = 2000000,
            UseHardwareAcceleration = true,
            Codec = VideoCodec.H264
        };
        _videoEncoder.Initialize(encoderConfig);
        
        // 3. 初始化输入模拟器
        _inputSimulator = new InputSimulator();
        
        // 4. 连接到中继服务器
        _networkClient = new RemoteControlClient();
        _networkClient.OnMouseEventReceived += OnMouseEventReceived;
        _networkClient.OnKeyboardEventReceived += OnKeyboardEventReceived;
        _networkClient.OnConnected += OnConnected;
        
        await _networkClient.ConnectAsync(_relayServerHost, _relayServerPort);
        
        // 5. 初始化网络质量监控
        _qualityMonitor = new NetworkQualityMonitor(_networkClient);
        _qualityMonitor.OnQualityChanged += OnNetworkQualityChanged;
        _qualityMonitor.Start();
        
        // 6. 初始化自适应码率控制
        _bitrateController = new BitrateController(_videoEncoder, _qualityMonitor);
        _bitrateController.Start();
        
        // 7. 开始捕获屏幕
        _screenCapturer.Start();
        
        Console.WriteLine("Agent started successfully");
    }
    
    private void OnConnected()
    {
        // 发送握手消息
        var handshakePacket = new HandshakePacket
        {
            DeviceId = GetDeviceId(),
            DeviceName = Environment.MachineName,
            ScreenWidth = Screen.PrimaryScreen.Bounds.Width,
            ScreenHeight = Screen.PrimaryScreen.Bounds.Height,
            ProtocolVersion = 1
        };
        
        // 发送...
    }
    
    private void OnFrameCaptured(CaptureFrame frame)
    {
        try
        {
            // 编码帧
            byte[] encodedData = _videoEncoder.Encode(frame, out bool isKeyFrame);
            
            if (encodedData != null && encodedData.Length > 0)
            {
                // 创建视频帧包
                var packet = new VideoFramePacket
                {
                    Header = new PacketHeader
                    {
                        Type = MessageType.VideoFrame,
                        Timestamp = (uint)frame.Timestamp,
                        SequenceNumber = GetNextSequenceNumber()
                    },
                    Width = frame.Width,
                    Height = frame.Height,
                    Codec = VideoCodec.H264,
                    Data = encodedData,
                    DirtyRects = frame.DirtyRects
                };
                
                // 发送到网络
                _networkClient.SendVideoFrameAsync(packet).Wait();
                
                // 统计
                _qualityMonitor.ReportFrameSent(encodedData.Length, isKeyFrame);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error encoding frame: {ex}");
        }
    }
    
    private void OnMouseEventReceived(MouseEventPacket packet)
    {
        switch (packet.Action)
        {
            case MouseAction.Move:
                _inputSimulator.SimulateMouseMove(packet.X, packet.Y);
                break;
                
            case MouseAction.Down:
                _inputSimulator.SimulateMouseClick(packet.Button, isDown: true);
                break;
                
            case MouseAction.Up:
                _inputSimulator.SimulateMouseClick(packet.Button, isDown: false);
                break;
                
            case MouseAction.Wheel:
                _inputSimulator.SimulateMouseWheel(packet.WheelDelta);
                break;
        }
    }
    
    private void OnKeyboardEventReceived(KeyboardEventPacket packet)
    {
        bool isDown = packet.Action == KeyAction.Down;
        _inputSimulator.SimulateKeyPress(packet.VirtualKeyCode, isDown);
    }
    
    private void OnNetworkQualityChanged(NetworkQuality quality)
    {
        Console.WriteLine($"Network quality: RTT={quality.RTT}ms, " +
                         $"Loss={quality.PacketLoss}%, " +
                         $"Bandwidth={quality.AvailableBandwidth / 1000}Kbps");
        
        // 根据网络质量调整参数
        if (quality.PacketLoss > 5)
        {
            // 降低帧率
            _screenCapturer.SetFrameRate(30);
        }
        else if (quality.RTT < 50 && quality.PacketLoss < 1)
        {
            // 提高帧率
            _screenCapturer.SetFrameRate(60);
        }
    }
    
    private string GetDeviceId()
    {
        // 生成唯一设备ID
        return Guid.NewGuid().ToString();
    }
    
    private uint GetNextSequenceNumber()
    {
        // 序列号生成
        return 0;
    }
    
    public void Stop()
    {
        _screenCapturer?.Stop();
        _qualityMonitor?.Stop();
        _bitrateController?.Stop();
        _networkClient?.DisconnectAsync().Wait();
        _videoEncoder?.Dispose();
    }
}
```

---

## 7. 控制端主程序（Controller）

```csharp
public class RemoteControlController
{
    private RemoteControlClient _networkClient;
    private FFmpegH264Decoder _videoDecoder;
    private VideoRenderer _videoRenderer;
    private InputCapture _inputCapture;
    
    public event Action<Bitmap> OnFrameDecoded;
    
    public async Task ConnectAsync(string deviceId, string relayServer = "relay.example.com")
    {
        // 1. 初始化视频解码器
        _videoDecoder = new FFmpegH264Decoder();
        _videoDecoder.Initialize();
        _videoDecoder.OnFrameDecoded += (frame) =>
        {
            // 渲染到屏幕
            _videoRenderer.Render(frame);
            OnFrameDecoded?.Invoke(frame);
        };
        
        // 2. 初始化渲染器
        _videoRenderer = new VideoRenderer();
        
        // 3. 初始化输入捕获
        _inputCapture = new InputCapture();
        _inputCapture.OnMouseEvent += OnLocalMouseEvent;
        _inputCapture.OnKeyboardEvent += OnLocalKeyboardEvent;
        _inputCapture.Start();
        
        // 4. 连接到中继服务器
        _networkClient = new RemoteControlClient();
        _networkClient.OnVideoFrameReceived += OnVideoFrameReceived;
        await _networkClient.ConnectAsync(relayServer, 8888);
        
        // 5. 发送连接请求
        await SendConnectionRequest(deviceId);
        
        Console.WriteLine($"Connected to device: {deviceId}");
    }
    
    private void OnVideoFrameReceived(VideoFramePacket packet)
    {
        try
        {
            // 解码视频帧
            _videoDecoder.Decode(packet.Data);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error decoding frame: {ex}");
        }
    }
    
    private void OnLocalMouseEvent(int x, int y, MouseButton button, MouseAction action)
    {
        // 转换坐标（控制端窗口 -> 被控端屏幕）
        var transformedPos = TransformCoordinates(x, y);
        
        var packet = new MouseEventPacket
        {
            Header = new PacketHeader
            {
                Type = action == MouseAction.Move ? 
                       MessageType.MouseMove : MessageType.MouseClick,
                Timestamp = GetTimestamp()
            },
            X = transformedPos.X,
            Y = transformedPos.Y,
            Button = button,
            Action = action
        };
        
        _networkClient.SendMouseEventAsync(packet);
    }
    
    private void OnLocalKeyboardEvent(ushort virtualKeyCode, KeyAction action)
    {
        var packet = new KeyboardEventPacket
        {
            Header = new PacketHeader
            {
                Type = MessageType.KeyPress,
                Timestamp = GetTimestamp()
            },
            VirtualKeyCode = virtualKeyCode,
            Action = action
        };
        
        _networkClient.SendKeyboardEventAsync(packet);
    }
    
    private Point TransformCoordinates(int x, int y)
    {
        // 坐标转换逻辑（考虑缩放、偏移等）
        return new Point(x, y);
    }
    
    private async Task SendConnectionRequest(string deviceId)
    {
        // 发送连接请求到中继服务器
    }
    
    private uint GetTimestamp()
    {
        return (uint)Environment.TickCount;
    }
    
    public void Disconnect()
    {
        _inputCapture?.Stop();
        _networkClient?.DisconnectAsync().Wait();
        _videoDecoder?.Dispose();
    }
}
```

---

## 8. 网络质量监控与自适应码率

```csharp
// 网络质量监控器
public class NetworkQualityMonitor
{
    private RemoteControlClient _client;
    private Queue<long> _rttSamples = new Queue<long>();
    private Queue<double> _lossSamples = new Queue<double>();
    private Timer _monitorTimer;
    
    public event Action<NetworkQuality> OnQualityChanged;
    
    private long _lastSentTime;
    private int _framesSent;
    private int _framesReceived;
    private long _bytesSent;
    
    public void Start()
    {
        _monitorTimer = new Timer(MonitorNetwork, null, 0, 1000);
    }
    
    private void MonitorNetwork(object state)
    {
        // 1. 计算RTT（发送心跳包）
        long rtt = MeasureRTT();
        _rttSamples.Enqueue(rtt);
        if (_rttSamples.Count > 10) _rttSamples.Dequeue();
        
        // 2. 计算丢包率
        double packetLoss = CalculatePacketLoss();
        _lossSamples.Enqueue(packetLoss);
        if (_lossSamples.Count > 10) _lossSamples.Dequeue();
        
        // 3. 估算可用带宽
        long bandwidth = EstimateBandwidth();
        
        // 4. 构建质量报告
        var quality = new NetworkQuality
        {
            RTT = (int)_rttSamples.Average(),
            PacketLoss = _lossSamples.Average(),
            AvailableBandwidth = bandwidth,
            Jitter = CalculateJitter()
        };
        
        OnQualityChanged?.Invoke(quality);
    }
    
    private long MeasureRTT()
    {
        var stopwatch = Stopwatch.StartNew();
        
        // 发送ping包
        var pingPacket = new PacketHeader
        {
            Type = MessageType.Heartbeat,
            Timestamp = (uint)Environment.TickCount
        };
        
        // 等待响应...（简化）
        
        return stopwatch.ElapsedMilliseconds;
    }
    
    private double CalculatePacketLoss()
    {
        if (_framesSent == 0) return 0;
        
        int lostFrames = _framesSent - _framesReceived;
        double loss = (double)lostFrames / _framesSent * 100;
        
        // 重置计数器
        _framesSent = 0;
        _framesReceived = 0;
        
        return Math.Max(0, loss);
    }
    
    private long EstimateBandwidth()
    {
        long elapsed = Environment.TickCount64 - _lastSentTime;
        if (elapsed == 0) return 0;
        
        long bandwidth = (_bytesSent * 8 * 1000) / elapsed; // bps
        
        _bytesSent = 0;
        _lastSentTime = Environment.TickCount64;
        
        return bandwidth;
    }
    
    private int CalculateJitter()
    {
        if (_rttSamples.Count < 2) return 0;
        
        var rttArray = _rttSamples.ToArray();
        double variance = 0;
        
        for (int i = 1; i < rttArray.Length; i++)
        {
            variance += Math.Abs(rttArray[i] - rttArray[i - 1]);
        }
        
        return (int)(variance / (rttArray.Length - 1));
    }
    
    public void ReportFrameSent(int bytes, bool isKeyFrame)
    {
        _framesSent++;
        _bytesSent += bytes;
    }
    
    public void ReportFrameReceived()
    {
        _framesReceived++;
    }
    
    public void Stop()
    {
        _monitorTimer?.Dispose();
    }
}

// 网络质量数据
public class NetworkQuality
{
    public int RTT { get; set; }              // 往返时延(ms)
    public double PacketLoss { get; set; }     // 丢包率(%)
    public long AvailableBandwidth { get; set; }  // 可用带宽(bps)
    public int Jitter { get; set; }            // 抖动(ms)
}

// 自适应码率控制器
public class BitrateController
{
    private IVideoEncoder _encoder;
    private NetworkQualityMonitor _monitor;
    
    private int _currentBitrate = 2000000; // 2Mbps
    private int _minBitrate = 500000;      // 500Kbps
    private int _maxBitrate = 10000000;    // 10Mbps
    
    public void Start()
    {
        _monitor.OnQualityChanged += OnQualityChanged;
    }
    
    private void OnQualityChanged(NetworkQuality quality)
    {
        int targetBitrate = CalculateTargetBitrate(quality);
        
        if (Math.Abs(targetBitrate - _currentBitrate) > _currentBitrate * 0.1)
        {
            // 变化超过10%才调整
            _currentBitrate = targetBitrate;
            _encoder.SetBitrate(_currentBitrate);
            
            Console.WriteLine($"Bitrate adjusted to: {_currentBitrate / 1000}Kbps");
        }
    }
    
    private int CalculateTargetBitrate(NetworkQuality quality)
    {
        // 基于带宽的目标码率（使用80%带宽）
        int bandwidthBasedBitrate = (int)(quality.AvailableBandwidth * 0.8);
        
        // 基于丢包率的调整
        double lossMultiplier = quality.PacketLoss switch
        {
            < 1 => 1.0,
            < 3 => 0.9,
            < 5 => 0.7,
            < 10 => 0.5,
            _ => 0.3
        };
        
        // 基于RTT的调整
        double rttMultiplier = quality.RTT switch
        {
            < 50 => 1.0,
            < 100 => 0.9,
            < 200 => 0.8,
            _ => 0.6
        };
        
        int targetBitrate = (int)(bandwidthBasedBitrate * lossMultiplier * rttMultiplier);
        
        // 限制范围
        return Math.Clamp(targetBitrate, _minBitrate, _maxBitrate);
    }
    
    public void Stop()
    {
        _monitor.OnQualityChanged -= OnQualityChanged;
    }
}
```

---

## 9. 中继服务器（SignalR实现）

```csharp
using Microsoft.AspNetCore.SignalR;

// SignalR Hub（信令服务器）
public class RemoteControlHub : Hub
{
    private static ConcurrentDictionary<string, DeviceInfo> _onlineDevices = new();
    private static ConcurrentDictionary<string, string> _activeSessions = new();
    
    // 设备注册
    public async Task RegisterDevice(string deviceId, string deviceName, DeviceType type)
    {
        var device = new DeviceInfo
        {
            DeviceId = deviceId,
            DeviceName = deviceName,
            Type = type,
            ConnectionId = Context.ConnectionId,
            LastHeartbeat = DateTime.UtcNow
        };
        
        _onlineDevices[deviceId] = device;
        
        await Clients.Caller.SendAsync("RegisterSuccess", deviceId);
        
        Console.WriteLine($"Device registered: {deviceId} ({deviceName})");
    }
    
    // 请求连接到设备
    public async Task RequestConnection(string targetDeviceId)
    {
        if (!_onlineDevices.TryGetValue(targetDeviceId, out var targetDevice))
        {
            await Clients.Caller.SendAsync("ConnectionFailed", "Device not found");
            return;
        }
        
        // 通知被控端有连接请求
        await Clients.Client(targetDevice.ConnectionId)
            .SendAsync("ConnectionRequest", Context.ConnectionId);
    }
    
    // 接受连接
    public async Task AcceptConnection(string controllerConnectionId)
    {
        var sessionId = Guid.NewGuid().ToString();
        _activeSessions[sessionId] = Context.ConnectionId;
        
        // 通知双方建立P2P连接
        await Clients.Client(controllerConnectionId)
            .SendAsync("ConnectionAccepted", sessionId, Context.ConnectionId);
        
        await Clients.Caller.SendAsync("ConnectionEstablished", sessionId);
    }
    
    // ICE候选交换（WebRTC NAT穿透）
    public async Task SendIceCandidate(string targetConnectionId, string candidate)
    {
        await Clients.Client(targetConnectionId)
            .SendAsync("ReceiveIceCandidate", candidate);
    }
    
    // SDP Offer/Answer交换
    public async Task SendOffer(string targetConnectionId, string sdp)
    {
        await Clients.Client(targetConnectionId)
            .SendAsync("ReceiveOffer", sdp);
    }
    
    public async Task SendAnswer(string targetConnectionId, string sdp)
    {
        await Clients.Client(targetConnectionId)
            .SendAsync("ReceiveAnswer", sdp);
    }
    
    // 心跳
    public async Task Heartbeat(string deviceId)
    {
        if (_onlineDevices.TryGetValue(deviceId, out var device))
        {
            device.LastHeartbeat = DateTime.UtcNow;
        }
        
        await Clients.Caller.SendAsync("HeartbeatAck");
    }
    
    // 断开连接
    public override async Task OnDisconnectedAsync(Exception exception)
    {
        // 清理设备
        var device = _onlineDevices.FirstOrDefault(x => x.Value.ConnectionId == Context.ConnectionId);
        if (device.Key != null)
        {
            _onlineDevices.TryRemove(device.Key, out _);
            Console.WriteLine($"Device disconnected: {device.Key}");
        }
        
        await base.OnDisconnectedAsync(exception);
    }
    
    // 获取在线设备列表
    public async Task<List<DeviceInfo>> GetOnlineDevices()
    {
        return _onlineDevices.Values.ToList();
    }
}

// 设备信息
public class DeviceInfo
{
    public string DeviceId { get; set; }
    public string DeviceName { get; set; }
    public DeviceType Type { get; set; }
    public string ConnectionId { get; set; }
    public DateTime LastHeartbeat { get; set; }
}

public enum DeviceType
{
    Agent,      // 被控端
    Controller  // 控制端
}

// ASP.NET Core Startup配置
public class Startup
{
    public void ConfigureServices(IServiceCollection services)
    {
        services.AddSignalR(options =>
        {
            options.EnableDetailedErrors = true;
            options.MaximumReceiveMessageSize = 10 * 1024 * 1024; // 10MB
            options.KeepAliveInterval = TimeSpan.FromSeconds(10);
            options.ClientTimeoutInterval = TimeSpan.FromSeconds(30);
        });
        
        services.AddCors(options =>
        {
            options.AddPolicy("AllowAll", builder =>
            {
                builder.AllowAnyOrigin()
                       .AllowAnyMethod()
                       .AllowAnyHeader();
            });
        });
    }
    
    public void Configure(IApplicationBuilder app)
    {
        app.UseCors("AllowAll");
        app.UseRouting();
        
        app.UseEndpoints(endpoints =>
        {
            endpoints.MapHub<RemoteControlHub>("/remotecontrol");
        });
    }
}
```

---

## 10. NAT穿透（STUN/TURN）

```csharp
// STUN客户端（简化）
public class StunClient
{
    private const string StunServer = "stun.l.google.com";
    private const int StunPort = 19302;
    
    public async Task<IPEndPoint> GetPublicEndpointAsync()
    {
        using var udpClient = new UdpClient();
        
        // 1. 构造STUN Binding Request
        byte[] request = BuildStunBindingRequest();
        
        // 2. 发送到STUN服务器
        await udpClient.SendAsync(request, request.Length, StunServer, StunPort);
        
        // 3. 接收响应
        var result = await udpClient.ReceiveAsync();
        
        // 4. 解析MAPPED-ADDRESS
        var publicEndpoint = ParseStunResponse(result.Buffer);
        
        return publicEndpoint;
    }
    
    private byte[] BuildStunBindingRequest()
    {
        // STUN消息格式
        // 0-1: Message Type (0x0001 = Binding Request)
        // 2-3: Message Length
        // 4-7: Magic Cookie (0x2112A442)
        // 8-19: Transaction ID
        
        var message = new byte[20];
        message[0] = 0x00;
        message[1] = 0x01;
        // ... 填充其他字段
        
        return message;
    }
    
    private IPEndPoint ParseStunResponse(byte[] response)
    {
        // 解析STUN响应，提取公网IP和端口
        // ...
        return new IPEndPoint(IPAddress.Any, 0);
    }
}

// ICE (Interactive Connectivity Establishment)
public class IceAgent
{
    private List<IceCandidate> _localCandidates = new();
    private List<IceCandidate> _remoteCandidates = new();
    
    public async Task GatherCandidatesAsync()
    {
        // 1. 收集Host候选（本地IP）
        var localIPs = GetLocalIPAddresses();
        foreach (var ip in localIPs)
        {
            _localCandidates.Add(new IceCandidate
            {
                Type = CandidateType.Host,
                IP = ip,
                Port = AllocateLocalPort(),
                Priority = CalculatePriority(CandidateType.Host)
            });
        }
        
        // 2. 收集Server Reflexive候选（STUN获取）
        var stunClient = new StunClient();
        var publicEndpoint = await stunClient.GetPublicEndpointAsync();
        _localCandidates.Add(new IceCandidate
        {
            Type = CandidateType.ServerReflexive,
            IP = publicEndpoint.Address.ToString(),
            Port = publicEndpoint.Port,
            Priority = CalculatePriority(CandidateType.ServerReflexive)
        });
        
        // 3. 收集Relay候选（TURN中继）
        // var relayEndpoint = await AllocateTurnRelay();
        // _localCandidates.Add(...)
    }
    
    public async Task<IceCandidate> PerformConnectivityCheckAsync()
    {
        // ICE连接检查（按优先级排序）
        var candidatePairs = GenerateCandidatePairs();
        
        foreach (var pair in candidatePairs.OrderByDescending(p => p.Priority))
        {
            if (await TestConnectivity(pair.Local, pair.Remote))
            {
                return pair.Local; // 返回可用的候选
            }
        }
        
        return null; // 连接失败
    }
    
    private async Task<bool> TestConnectivity(IceCandidate local, IceCandidate remote)
    {
        // 发送STUN Binding Request进行连通性测试
        // ...
        return false;
    }
    
    private List<IPAddress> GetLocalIPAddresses()
    {
        return NetworkInterface.GetAllNetworkInterfaces()
            .SelectMany(i => i.GetIPProperties().UnicastAddresses)
            .Where(a => a.Address.AddressFamily == AddressFamily.InterNetwork)
            .Select(a => a.Address)
            .ToList();
    }
    
    private int AllocateLocalPort()
    {
        return new Random().Next(49152, 65535);
    }
    
    private int CalculatePriority(CandidateType type)
    {
        return type switch
        {
            CandidateType.Host => 126,
            CandidateType.ServerReflexive => 100,
            CandidateType.Relay => 0,
            _ => 0
        };
    }
    
    private List<CandidatePair> GenerateCandidatePairs()
    {
        var pairs = new List<CandidatePair>();
        
        foreach (var local in _localCandidates)
        {
            foreach (var remote in _remoteCandidates)
            {
                pairs.Add(new CandidatePair
                {
                    Local = local,
                    Remote = remote,
                    Priority = Math.Min(local.Priority, remote.Priority)
                });
            }
        }
        
        return pairs;
    }
}

public class IceCandidate
{
    public CandidateType Type { get; set; }
    public string IP { get; set; }
    public int Port { get; set; }
    public int Priority { get; set; }
}

public enum CandidateType
{
    Host,              // 本地地址
    ServerReflexive,   // STUN获取的公网地址
    Relay              // TURN中继地址
}

public class CandidatePair
{
    public IceCandidate Local { get; set; }
    public IceCandidate Remote { get; set; }
    public int Priority { get; set; }
}
```

---

## 11. 主程序入口

```csharp
// 被控端启动
class AgentProgram
{
    static async Task Main(string[] args)
    {
        Console.WriteLine("=== Remote Control Agent ===");
        
        var agent = new RemoteControlAgent();
        
        try
        {
            await agent.StartAsync();
            
            Console.WriteLine("Press any key to stop...");
            Console.ReadKey();
            
            agent.Stop();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex}");
        }
    }
}

// 控制端启动
class ControllerProgram
{
    static async Task Main(string[] args)
    {
        Console.WriteLine("=== Remote Control Controller ===");
        Console.Write("Enter device ID to connect: ");
        string deviceId = Console.ReadLine();
        
        var controller = new RemoteControlController();
        
        try
        {
            await controller.ConnectAsync(deviceId);
            
            controller.OnFrameDecoded += (frame) =>
            {
                // 显示帧到窗口
                // DisplayFrame(frame);
            };
            
            Console.WriteLine("Connected! Press any key to disconnect...");
            Console.ReadKey();
            
            controller.Disconnect();
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex}");
        }
    }
}

// 中继服务器启动
class RelayServerProgram
{
    static async Task Main(string[] args)
    {
        var host = Host.CreateDefaultBuilder(args)
            .ConfigureWebHostDefaults(webBuilder =>
            {
                webBuilder.UseStartup<Startup>();
                webBuilder.UseUrls("http://0.0.0.0:8888");
            })
            .Build();
        
        Console.WriteLine("Relay Server started on port 8888");
        
        await host.RunAsync();
    }
}
```

---

## 总结：技术要点

### 性能优化关键点
1. **硬件加速编解码**：使用NVENC/QSV，延迟<10ms
2. **增量传输**：只发送变化区域（DirtyRects）
3. **自适应码率**：根据网络实时调整
4. **零拷贝**：使用GPU纹理直接编码

### 网络优化
1. **TCP+UDP混合**：控制用TCP，视频用UDP
2. **P2P直连优先**：STUN穿透成功率70%+
3. **TURN降级**：穿透失败时自动切换中继
4. **FEC纠错**：减少重传，降低延迟

### 延迟控制
- 捕获：5-10ms（DXGI）
- 编码：5-15ms（硬件）
- 传输：20-50ms（局域网/直连）
- 解码：5-10ms
- **总计：<100ms**

这套方案可以达到ToDesk级别的流畅度！
