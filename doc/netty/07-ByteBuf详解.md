# 07 - ByteBuf详解

ByteBuf是Netty的数据容器，相比Java NIO的ByteBuffer，它更强大、更易用。理解ByteBuf对于使用Netty至关重要。

## 1. 为什么需要ByteBuf？

### 1.1 Java ByteBuffer的问题

```java
// Java NIO的ByteBuffer使用起来很麻烦
ByteBuffer buffer = ByteBuffer.allocate(1024);

// 1. 写入数据
buffer.put("Hello".getBytes());

// 2. 切换到读模式（必须调用flip）
buffer.flip();

// 3. 读取数据
byte[] data = new byte[buffer.remaining()];
buffer.get(data);

// 4. 清空缓冲区（必须调用clear或compact）
buffer.clear();
```

**ByteBuffer的缺点：**
- ❌ 只有一个position指针，读写需要切换模式（flip/clear）
- ❌ 容量固定，不能动态扩容
- ❌ API不够友好
- ❌ 只能使用DirectBuffer或HeapBuffer，不能灵活切换

### 1.2 Netty ByteBuf的优势

```java
// Netty的ByteBuf使用简单
ByteBuf buffer = Unpooled.buffer(1024);

// 1. 写入数据（不需要flip）
buffer.writeBytes("Hello".getBytes());

// 2. 读取数据（读写独立）
byte[] data = new byte[buffer.readableBytes()];
buffer.readBytes(data);

// 3. 可以重复读取（使用readerIndex）
buffer.readerIndex(0);
buffer.readBytes(data);

// 4. 自动扩容
buffer.writeBytes("World".getBytes());  // 自动扩容
```

**ByteBuf的优势：**
- ✅ 读写索引分离，不需要flip
- ✅ 自动扩容
- ✅ 支持引用计数，自动内存管理
- ✅ 支持零拷贝（CompositeByteBuf、slice）
- ✅ 支持池化，减少GC压力

## 2. ByteBuf的结构

### 2.1 ByteBuf的组成

```
┌──────────────────┬──────────────────┬──────────────────┐
│  可丢弃字节       │   可读字节        │   可写字节        │
│  (已读)          │   (待读)         │   (空闲)         │
└──────────────────┴──────────────────┴──────────────────┘
0            readerIndex         writerIndex        capacity
                   │                   │
                   └─ readableBytes() ─┘
                                       │
                                       └──── writableBytes() ────┘
```

**三个重要指针：**
1. **readerIndex**：读指针
2. **writerIndex**：写指针
3. **capacity**：容量

### 2.2 ByteBuf的索引操作

```java
public class ByteBufIndexExample {
    
    public static void main(String[] args) {
        // 创建一个容量为10的ByteBuf
        ByteBuf buffer = Unpooled.buffer(10);
        
        // 初始状态
        System.out.println("初始状态:");
        printBufferInfo(buffer);
        // 输出: readerIndex=0, writerIndex=0, capacity=10
        
        // 写入5个字节
        buffer.writeBytes("Hello".getBytes());
        System.out.println("\n写入5个字节后:");
        printBufferInfo(buffer);
        // 输出: readerIndex=0, writerIndex=5, capacity=10
        
        // 读取3个字节
        byte[] data = new byte[3];
        buffer.readBytes(data);
        System.out.println("\n读取3个字节后:");
        printBufferInfo(buffer);
        // 输出: readerIndex=3, writerIndex=5, capacity=10
        
        // 丢弃已读字节
        buffer.discardReadBytes();
        System.out.println("\n丢弃已读字节后:");
        printBufferInfo(buffer);
        // 输出: readerIndex=0, writerIndex=2, capacity=10
        
        // 清空
        buffer.clear();
        System.out.println("\n清空后:");
        printBufferInfo(buffer);
        // 输出: readerIndex=0, writerIndex=0, capacity=10
    }
    
    private static void printBufferInfo(ByteBuf buffer) {
        System.out.println("readerIndex: " + buffer.readerIndex());
        System.out.println("writerIndex: " + buffer.writerIndex());
        System.out.println("capacity: " + buffer.capacity());
        System.out.println("maxCapacity: " + buffer.maxCapacity());
        System.out.println("readableBytes: " + buffer.readableBytes());
        System.out.println("writableBytes: " + buffer.writableBytes());
        System.out.println("isReadable: " + buffer.isReadable());
        System.out.println("isWritable: " + buffer.isWritable());
    }
}
```

## 3. ByteBuf的类型

### 3.1 按内存位置分类

```
ByteBuf
    │
    ├── HeapByteBuf        ← 堆内存
    │   └── 优点：分配快，GC管理
    │       缺点：IO操作时需要复制到直接内存
    │
    └── DirectByteBuf      ← 直接内存(堆外)
        └── 优点：IO操作快，零拷贝
            缺点：分配慢，需要手动管理
```

**示例代码：**

```java
public class ByteBufTypeExample {
    
    public static void main(String[] args) {
        // 1. 堆内存ByteBuf
        ByteBuf heapBuffer = Unpooled.buffer(256);
        System.out.println("堆内存: " + heapBuffer.hasArray());  // true
        
        if (heapBuffer.hasArray()) {
            byte[] array = heapBuffer.array();
            int offset = heapBuffer.arrayOffset() + heapBuffer.readerIndex();
            int length = heapBuffer.readableBytes();
            System.out.println("可以直接访问数组: offset=" + offset + ", length=" + length);
        }
        
        // 2. 直接内存ByteBuf
        ByteBuf directBuffer = Unpooled.directBuffer(256);
        System.out.println("直接内存: " + directBuffer.hasArray());  // false
        System.out.println("直接内存: " + directBuffer.isDirect());  // true
        
        if (!directBuffer.hasArray()) {
            System.out.println("不能直接访问数组，需要复制");
            int length = directBuffer.readableBytes();
            byte[] array = new byte[length];
            directBuffer.getBytes(directBuffer.readerIndex(), array);
        }
        
        // 释放
        heapBuffer.release();
        directBuffer.release();
    }
}
```

**选择建议：**

| 场景 | 推荐类型 | 原因 |
|------|---------|------|
| 网络IO | DirectByteBuf | 零拷贝，性能好 |
| 内存数据处理 | HeapByteBuf | 访问快，GC管理 |
| 默认 | 池化DirectByteBuf | 平衡性能和内存 |

### 3.2 按是否池化分类

```
ByteBuf
    │
    ├── UnpooledByteBuf          ← 非池化
    │   └── 每次都新建，用完就GC
    │
    └── PooledByteBuf            ← 池化(推荐)
        └── 从池中获取，用完归还，减少GC
```

**示例代码：**

```java
public class PooledByteBufExample {
    
    public static void main(String[] args) {
        // 1. 非池化ByteBuf
        ByteBuf unpooled = UnpooledByteBufAllocator.DEFAULT.buffer(256);
        System.out.println("非池化: " + unpooled.getClass().getSimpleName());
        
        // 2. 池化ByteBuf（推荐）
        ByteBuf pooled = PooledByteBufAllocator.DEFAULT.buffer(256);
        System.out.println("池化: " + pooled.getClass().getSimpleName());
        
        // 使用
        pooled.writeBytes("Hello".getBytes());
        System.out.println("写入数据");
        
        // 释放（池化ByteBuf会归还到池中）
        pooled.release();
        System.out.println("释放完成");
        
        // 再次获取（可能是同一个对象）
        ByteBuf pooled2 = PooledByteBufAllocator.DEFAULT.buffer(256);
        System.out.println("再次获取: " + (pooled == pooled2));
        pooled2.release();
    }
}
```

## 4. ByteBuf的创建

### 4.1 创建方式

```java
public class ByteBufCreation {
    
    public static void main(String[] args) {
        // 方式1：Unpooled工具类（简单，不推荐生产）
        ByteBuf buf1 = Unpooled.buffer(256);                    // 堆内存
        ByteBuf buf2 = Unpooled.directBuffer(256);              // 直接内存
        ByteBuf buf3 = Unpooled.copiedBuffer("Hello", CharsetUtil.UTF_8);
        ByteBuf buf4 = Unpooled.wrappedBuffer("World".getBytes());
        
        // 方式2：ByteBufAllocator（推荐）
        ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        ByteBuf buf5 = allocator.buffer();                      // 默认容量256
        ByteBuf buf6 = allocator.buffer(1024);                  // 指定容量
        ByteBuf buf7 = allocator.heapBuffer(256);               // 堆内存
        ByteBuf buf8 = allocator.directBuffer(256);             // 直接内存
        ByteBuf buf9 = allocator.compositeBuffer();             // 组合Buffer
        
        // 方式3：从Channel获取Allocator
        // ctx.alloc().buffer();
        
        // 方式4：从配置获取
        ChannelConfig config = null; // channel.config();
        if (config != null) {
            ByteBufAllocator allocator2 = config.getAllocator();
            ByteBuf buf10 = allocator2.buffer();
        }
        
        // 释放
        buf1.release();
        buf5.release();
        // ... 其他buffer也要释放
    }
}
```

**推荐创建方式：**

```java
// ✅ 推荐：使用PooledByteBufAllocator
ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
ByteBuf buffer = allocator.buffer(1024);

// ✅ 推荐：从ChannelHandlerContext获取
@Override
public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf response = ctx.alloc().buffer(1024);
    response.writeBytes("Response".getBytes());
    ctx.writeAndFlush(response);
}
```

## 5. ByteBuf的读写操作

### 5.1 写入操作

```java
public class ByteBufWriteExample {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.buffer(256);
        
        // 1. 写入基本类型
        buffer.writeBoolean(true);                      // 1字节
        buffer.writeByte(0x01);                         // 1字节
        buffer.writeShort(100);                         // 2字节
        buffer.writeInt(10000);                         // 4字节
        buffer.writeLong(100000L);                      // 8字节
        buffer.writeFloat(3.14f);                       // 4字节
        buffer.writeDouble(3.14159);                    // 8字节
        buffer.writeChar('A');                          // 2字节
        
        // 2. 写入字节数组
        buffer.writeBytes("Hello".getBytes());
        
        // 3. 写入另一个ByteBuf
        ByteBuf other = Unpooled.copiedBuffer("World", CharsetUtil.UTF_8);
        buffer.writeBytes(other);
        other.release();
        
        // 4. 写入字符串（需要指定字符集）
        buffer.writeCharSequence("Netty", CharsetUtil.UTF_8);
        
        // 5. 设置指定位置（不移动writerIndex）
        buffer.setByte(0, 0xFF);
        buffer.setInt(1, 12345);
        
        System.out.println("写入完成，writerIndex: " + buffer.writerIndex());
        
        buffer.release();
    }
}
```

### 5.2 读取操作

```java
public class ByteBufReadExample {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.buffer(256);
        
        // 准备数据
        buffer.writeBoolean(true);
        buffer.writeByte(0x01);
        buffer.writeShort(100);
        buffer.writeInt(10000);
        buffer.writeLong(100000L);
        buffer.writeBytes("Hello".getBytes());
        
        // 读取数据
        System.out.println("可读字节数: " + buffer.readableBytes());
        
        boolean bool = buffer.readBoolean();            // 1字节
        byte b = buffer.readByte();                     // 1字节
        short s = buffer.readShort();                   // 2字节
        int i = buffer.readInt();                       // 4字节
        long l = buffer.readLong();                     // 8字节
        
        byte[] bytes = new byte[5];
        buffer.readBytes(bytes);                        // 读取到数组
        String str = new String(bytes);
        
        System.out.println("boolean: " + bool);
        System.out.println("byte: " + b);
        System.out.println("short: " + s);
        System.out.println("int: " + i);
        System.out.println("long: " + l);
        System.out.println("string: " + str);
        
        // 获取指定位置（不移动readerIndex）
        buffer.readerIndex(0);
        boolean bool2 = buffer.getBoolean(0);
        int i2 = buffer.getInt(4);
        
        System.out.println("get方法不影响readerIndex: " + buffer.readerIndex());
        
        buffer.release();
    }
}
```

### 5.3 读写方法对比

| 操作 | read*/write* | get*/set* |
|------|-------------|-----------|
| 是否移动索引 | ✅ 是 | ❌ 否 |
| 是否检查越界 | ✅ 是 | ✅ 是 |
| 适用场景 | 顺序读写 | 随机访问 |

## 6. ByteBuf的高级特性

### 6.1 引用计数（Reference Counting）

ByteBuf使用引用计数来管理内存：

```java
public class ReferenceCountExample {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.buffer(256);
        
        // 1. 查看引用计数
        System.out.println("初始引用计数: " + buffer.refCnt());  // 1
        
        // 2. 增加引用计数
        buffer.retain();
        System.out.println("调用retain后: " + buffer.refCnt());  // 2
        
        buffer.retain(2);
        System.out.println("调用retain(2)后: " + buffer.refCnt());  // 4
        
        // 3. 减少引用计数
        buffer.release();
        System.out.println("调用release后: " + buffer.refCnt());  // 3
        
        buffer.release(2);
        System.out.println("调用release(2)后: " + buffer.refCnt());  // 1
        
        // 4. 最后一次release会真正释放内存
        buffer.release();
        System.out.println("最后release后: " + buffer.refCnt());  // 0
        
        // 5. 释放后不能再使用
        try {
            buffer.writeByte(1);  // 抛出异常
        } catch (IllegalReferenceCountException e) {
            System.out.println("已释放，不能再使用");
        }
    }
}
```

**引用计数规则：**
1. 谁创建谁释放
2. 谁retain谁release
3. 引用计数为0时自动回收

### 6.2 零拷贝（Zero-Copy）

#### CompositeByteBuf（组合Buffer）

```java
public class CompositeByteBufExample {
    
    public static void main(String[] args) {
        // 传统方式：需要复制（慢）
        ByteBuf header = Unpooled.copiedBuffer("Header", CharsetUtil.UTF_8);
        ByteBuf body = Unpooled.copiedBuffer("Body", CharsetUtil.UTF_8);
        
        ByteBuf traditional = Unpooled.buffer(
            header.readableBytes() + body.readableBytes());
        traditional.writeBytes(header);  // 复制
        traditional.writeBytes(body);    // 复制
        
        System.out.println("传统方式: " + traditional.toString(CharsetUtil.UTF_8));
        
        // Netty方式：零拷贝（快）
        CompositeByteBuf composite = Unpooled.compositeBuffer();
        composite.addComponents(true, header, body);  // 只是引用，不复制
        
        System.out.println("零拷贝方式: " + composite.toString(CharsetUtil.UTF_8));
        
        // 可以像普通ByteBuf一样使用
        System.out.println("可读字节: " + composite.readableBytes());
        System.out.println("组件数量: " + composite.numComponents());
        
        // 释放
        traditional.release();
        composite.release();
    }
}
```

#### Slice（切片）

```java
public class SliceExample {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8);
        
        // 切片：共享同一块内存，不复制
        ByteBuf slice1 = buffer.slice(0, 5);      // "Hello"
        ByteBuf slice2 = buffer.slice(6, 5);      // "World"
        
        System.out.println("Slice1: " + slice1.toString(CharsetUtil.UTF_8));
        System.out.println("Slice2: " + slice2.toString(CharsetUtil.UTF_8));
        
        // 修改slice会影响原buffer
        slice1.setByte(0, 'h');
        System.out.println("修改slice后: " + buffer.toString(CharsetUtil.UTF_8));
        // 输出: hello World
        
        // 释放（只需要释放原buffer）
        buffer.release();
        // slice不需要release（它们只是引用）
    }
}
```

#### Duplicate（复制）

```java
public class DuplicateExample {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.copiedBuffer("Netty", CharsetUtil.UTF_8);
        
        // duplicate：共享数据，但有独立的索引
        ByteBuf duplicate = buffer.duplicate();
        
        // 独立的读写索引
        buffer.readByte();
        System.out.println("原buffer readerIndex: " + buffer.readerIndex());      // 1
        System.out.println("duplicate readerIndex: " + duplicate.readerIndex());  // 0
        
        // 但数据是共享的
        buffer.setByte(0, 'n');
        System.out.println("修改原buffer: " + duplicate.toString(CharsetUtil.UTF_8));
        // 输出: netty
        
        buffer.release();
    }
}
```

### 6.3 ByteBuf的转换

```java
public class ByteBufConversion {
    
    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.copiedBuffer("Hello Netty", CharsetUtil.UTF_8);
        
        // 1. ByteBuf转String
        String str1 = buffer.toString(CharsetUtil.UTF_8);
        System.out.println("toString: " + str1);
        
        String str2 = buffer.toString(0, 5, CharsetUtil.UTF_8);  // 指定范围
        System.out.println("toString(range): " + str2);
        
        // 2. ByteBuf转byte[]
        byte[] array = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), array);
        System.out.println("to byte[]: " + new String(array));
        
        // 3. ByteBuf转NIO ByteBuffer
        ByteBuffer nioBuffer = buffer.nioBuffer();
        System.out.println("to ByteBuffer: " + nioBuffer);
        
        // 4. 如果是HeapByteBuf，可以直接访问数组
        if (buffer.hasArray()) {
            byte[] backingArray = buffer.array();
            int offset = buffer.arrayOffset() + buffer.readerIndex();
            int length = buffer.readableBytes();
            System.out.println("直接访问数组: " + new String(backingArray, offset, length));
        }
        
        buffer.release();
    }
}
```

## 7. 实战：自定义协议编解码

```java
/**
 * 自定义协议格式：
 * +------+--------+----------+
 * | 魔数 | 长度   | 数据     |
 * | 4字节| 4字节  | N字节    |
 * +------+--------+----------+
 */
public class CustomProtocol {
    
    private static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    /**
     * 编码
     */
    public static ByteBuf encode(String message) {
        byte[] data = message.getBytes(CharsetUtil.UTF_8);
        
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(MAGIC_NUMBER);      // 魔数
        buffer.writeInt(data.length);       // 长度
        buffer.writeBytes(data);            // 数据
        
        return buffer;
    }
    
    /**
     * 解码
     */
    public static String decode(ByteBuf buffer) {
        // 检查魔数
        int magic = buffer.readInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("无效的魔数: " + magic);
        }
        
        // 读取长度
        int length = buffer.readInt();
        
        // 检查数据完整性
        if (buffer.readableBytes() < length) {
            throw new IllegalArgumentException("数据不完整");
        }
        
        // 读取数据
        byte[] data = new byte[length];
        buffer.readBytes(data);
        
        return new String(data, CharsetUtil.UTF_8);
    }
    
    public static void main(String[] args) {
        // 编码
        String message = "Hello Netty Protocol!";
        ByteBuf encoded = encode(message);
        System.out.println("编码后长度: " + encoded.readableBytes());
        
        // 解码
        String decoded = decode(encoded);
        System.out.println("解码结果: " + decoded);
        
        // 释放
        encoded.release();
    }
}
```

## 8. 内存泄漏检测

### 8.1 启用检测

```java
// 在JVM启动参数中添加：
// -Dio.netty.leakDetection.level=ADVANCED

// 或在代码中设置：
ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
```

**检测级别：**

| 级别 | 说明 | 性能影响 | 适用场景 |
|------|------|---------|---------|
| DISABLED | 禁用 | 无 | 生产环境 |
| SIMPLE | 简单检测 | 很小 | 生产环境 |
| ADVANCED | 详细检测 | 小 | 测试环境 |
| PARANOID | 每次都检测 | 大 | 调试 |

### 8.2 检测示例

```java
public class LeakDetectionExample {
    
    public static void main(String[] args) {
        // 设置检测级别
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        
        // 内存泄漏示例（没有release）
        ByteBuf leaked = Unpooled.buffer(256);
        leaked.writeBytes("This buffer is leaked!".getBytes());
        // 忘记调用 leaked.release();
        
        // 手动触发GC
        System.gc();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Netty会打印泄漏警告：
        // LEAK: ByteBuf.release() was not called before it's garbage-collected.
    }
}
```

## 9. 最佳实践

### 9.1 总是记得释放

```java
// ❌ 错误：忘记释放
public void bad() {
    ByteBuf buffer = Unpooled.buffer(256);
    buffer.writeBytes("data".getBytes());
    // 忘记 buffer.release();
}

// ✅ 正确：使用try-finally
public void good1() {
    ByteBuf buffer = Unpooled.buffer(256);
    try {
        buffer.writeBytes("data".getBytes());
        // 使用buffer
    } finally {
        buffer.release();
    }
}

// ✅ 更好：使用SimpleChannelInboundHandler
public class GoodHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // 自动释放，不需要手动管理
    }
}
```

### 9.2 使用池化ByteBuf

```java
// ✅ 推荐：使用池化
ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
ByteBuf buffer = allocator.buffer(1024);

// ❌ 不推荐：非池化
ByteBuf buffer2 = Unpooled.buffer(1024);
```

### 9.3 避免数据复制

```java
// ❌ 低效：复制数据
ByteBuf header = ...;
ByteBuf body = ...;
ByteBuf combined = Unpooled.buffer(header.readableBytes() + body.readableBytes());
combined.writeBytes(header);
combined.writeBytes(body);

// ✅ 高效：零拷贝
CompositeByteBuf combined = Unpooled.compositeBuffer();
combined.addComponents(true, header, body);
```

## 10. 小结

本章我们深入学习了ByteBuf：

✅ ByteBuf的优势和结构  
✅ ByteBuf的类型（堆/直接、池化/非池化）  
✅ ByteBuf的创建和读写  
✅ 引用计数机制  
✅ 零拷贝技术  
✅ 内存泄漏检测  

**核心要点：**
1. ByteBuf比ByteBuffer更强大、更易用
2. 读写索引分离，不需要flip
3. 使用引用计数管理内存
4. 使用池化ByteBuf减少GC
5. 总是记得release

## 11. 练习题

1. **基础练习**：使用ByteBuf实现一个简单的编解码器
2. **进阶练习**：对比HeapByteBuf和DirectByteBuf的性能
3. **挑战练习**：实现一个自定义的ByteBuf池

## 12. 下一步

下一章我们将学习编解码器，学习如何处理半包粘包问题！🚀
