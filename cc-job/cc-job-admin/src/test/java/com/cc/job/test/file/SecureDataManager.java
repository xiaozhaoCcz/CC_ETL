package com.cc.job.test.file;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class SecureDataManager {

    // 数据模型类
    public static class AppData implements Serializable {
        private static final long serialVersionUID = 1L;
        private Map<String,String> items = new HashMap<>(3);
        private String configValue;

        public void addItem(String key,String item) {
            items.put(key,item);
        }

        public void removeItem(String key) {
            items.remove(key);
        }

        @Override
        public String toString() {
            return "AppData{" +
                    "items=" + items +
                    ", configValue='" + configValue + '\'' +
                    '}';
        }
    }

    // 数据管理器 - 处理加密的导入导出
    public static class DataManager {
        private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
        private static final int KEY_SIZE = 256;
        private static final int IV_SIZE = 16;

        // 使用密码派生密钥
        public static SecretKey deriveKey(String password) throws NoSuchAlgorithmException {
            // 在实际应用中，应使用密钥派生函数如PBKDF2
            byte[] keyBytes = password.getBytes(StandardCharsets.UTF_8);
            byte[] paddedKeyBytes = new byte[KEY_SIZE / 8];
            System.arraycopy(keyBytes, 0, paddedKeyBytes, 0, Math.min(keyBytes.length, paddedKeyBytes.length));
            return new SecretKeySpec(paddedKeyBytes, "AES");
        }

        // 加密并导出数据
        public static void exportData(AppData data, File file, String password) throws Exception {
            SecretKey key = deriveKey(password);

            // 生成随机IV
            SecureRandom random = new SecureRandom();
            byte[] ivBytes = new byte[IV_SIZE];
            random.nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            try (FileOutputStream fos = new FileOutputStream(file);
                 // 将IV写入文件开头
                 DataOutputStream dos = new DataOutputStream(fos)) {

                // 写入IV
                dos.write(ivBytes);

                // 创建加密输出流
                try (CipherOutputStream cos = new CipherOutputStream(dos, cipher);
                     ObjectOutputStream oos = new ObjectOutputStream(cos)) {

                    // 写入序列化对象
                    oos.writeObject(data);
                }
            }
        }

        // 导入并解密数据
        public static AppData importData(File file, String password) throws Exception {
            SecretKey key = deriveKey(password);

            try (FileInputStream fis = new FileInputStream(file);
                 DataInputStream dis = new DataInputStream(fis)) {

                // 读取文件开头的IV
                byte[] ivBytes = new byte[IV_SIZE];
                dis.readFully(ivBytes);
                IvParameterSpec iv = new IvParameterSpec(ivBytes);

                // 初始化解密器
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, key, iv);

                // 创建解密输入流
                try (CipherInputStream cis = new CipherInputStream(dis, cipher);
                     ObjectInputStream ois = new ObjectInputStream(cis)) {

                    // 读取并反序列化对象
                    return (AppData) ois.readObject();
                }
            }
        }

        // 生成安全的随机密码
        public static String generateSecurePassword(int length) {
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            return Base64.getEncoder().encodeToString(bytes).substring(0, length);
        }
    }

    public static void main(String[] args) {
        try {
            // 1. 创建示例数据
            AppData data = new AppData();
            data.addItem("1","项目1");
            data.addItem("2","项目2");
            data.addItem("3","项目3");
            data.configValue = "重要配置值";

            System.out.println("原始数据: " + data);

            // 2. 设置文件路径和密码
            String password = DataManager.generateSecurePassword(16);
            System.out.println("生成的加密密码: " + password);

            File exportFile = new File("secure_data.mydata");

            // 3. 导出加密数据
            DataManager.exportData(data, exportFile, password);
            System.out.println("数据已加密导出到: " + exportFile.getAbsolutePath());
            System.out.println("文件大小: " + exportFile.length() + " 字节");

            // 4. 导入加密数据
            AppData importedData = DataManager.importData(exportFile, password);
            System.out.println("\n导入的数据: " + importedData);

            // 5. 测试错误密码
            try {
                System.out.println("\n尝试使用错误密码导入...");
                DataManager.importData(exportFile, "wrong_password");
            } catch (Exception e) {
                System.out.println("错误密码导入失败: " + e.getClass().getSimpleName());
                System.out.println("错误信息: " + e.getMessage());
            }

            // 6. 测试文件损坏
            try {
                System.out.println("\n尝试导入损坏的文件...");
                corruptFile(exportFile);
                DataManager.importData(exportFile, password);
            } catch (Exception e) {
                System.out.println("损坏文件导入失败: " + e.getClass().getSimpleName());
                System.out.println("错误信息: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 测试方法：损坏文件
    private static void corruptFile(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            // 移动到文件中间位置
            raf.seek(file.length() / 2);
            // 写入随机字节破坏文件
            raf.write(new SecureRandom().nextInt());
        }
    }
}
