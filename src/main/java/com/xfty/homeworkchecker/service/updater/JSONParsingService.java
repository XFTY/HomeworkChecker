package com.xfty.homeworkchecker.service.updater;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSONParsingService — JSON 解析服务
 * <p>
 * 从 GitHub Release API 返回的 JSON 中提取 tag_name、name、body、
 * assets 下载链接、版本类型检测。所有方法均为静态工具方法。
 * </p>
 */
public class JSONParsingService {
    
    /**
     * 从GitHub Release JSON字符串中提取tag名称
     * @param jsonString GitHub API返回的JSON字符串
     * @return tag名称，例如 "1.6.0"
     */
    public static String extractTagName(String jsonString) {
        JSONObject jsonObject = JSON.parseObject(jsonString);
        return jsonObject.getString("tag_name");
    }
    
    /**
     * 从GitHub Release JSON字符串中提取发行版标题
     * @param jsonString GitHub API返回的JSON字符串
     * @return 发行版标题，例如 "1.6.0 - Beta"
     */
    public static String extractReleaseTitle(String jsonString) {
        JSONObject jsonObject = JSON.parseObject(jsonString);
        return jsonObject.getString("name");
    }
    
    /**
     * 从GitHub Release JSON字符串中提取发行版描述
     * @param jsonString GitHub API返回的JSON字符串
     * @return 发行版描述内容
     */
    public static String extractReleaseBody(String jsonString) {
        JSONObject jsonObject = JSON.parseObject(jsonString);
        return jsonObject.getString("body");
    }
    
    /**
     * 从GitHub Release JSON字符串中提取所有下载链接
     * @param jsonString GitHub API返回的JSON字符串
     * @return 下载链接列表
     */
    public static List<String> extractDownloadUrls(String jsonString) {
        List<String> downloadUrls = new ArrayList<>();
        JSONObject jsonObject = JSON.parseObject(jsonString);
        JSONArray assets = jsonObject.getJSONArray("assets");
        
        if (assets != null) {
            for (int i = 0; i < assets.size(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String downloadUrl = asset.getString("browser_download_url");
                if (downloadUrl != null && !downloadUrl.isEmpty()) {
                    downloadUrls.add(downloadUrl);
                }
            }
        }
        
        return downloadUrls;
    }
    
    /**
     * 检测发行版标题中的版本类型标签
     * @param jsonString GitHub API返回的JSON字符串
     * @return 版本类型标签："Beta"、"Alpha"、"Snapshot" 或 "release"
     */
    public static String extractReleaseType(String jsonString) {
        String title = extractReleaseTitle(jsonString);
        if (title == null || title.isEmpty()) {
            return "release";
        }
        
        String lowerTitle = title.toLowerCase();
        
        if (lowerTitle.contains("beta")) {
            return "Beta";
        } else if (lowerTitle.contains("alpha")) {
            return "Alpha";
        } else if (lowerTitle.contains("snapshot")) {
            return "Snapshot";
        } else {
            return "release";
        }
    }
    
    /**
     * 测试方法：从本地文件读取并解析GitHub Release JSON
     * @param jsonFilePath JSON文件路径
     * @throws IOException 文件读取异常
     */
    public static void testParseFromFile(String jsonFilePath) throws IOException {
        Path path = Path.of(jsonFilePath);
        String jsonString = Files.readString(path);
        
        System.out.println("=== GitHub Release 信息解析测试 ===");
        
        // 提取tag名称
        String tagName = extractTagName(jsonString);
        System.out.println("Tag名称: " + tagName);
        
        // 提取发行版标题
        String releaseTitle = extractReleaseTitle(jsonString);
        System.out.println("发行版标题: " + releaseTitle);
        
        // 提取发行版描述
        String body = extractReleaseBody(jsonString);
        System.out.println("\n发行版描述（前200字符）: ");
        if (body != null && body.length() > 200) {
            System.out.println(body.substring(0, 200) + "...");
        } else {
            System.out.println(body);
        }
        
        // 提取下载链接
        List<String> downloadUrls = extractDownloadUrls(jsonString);
        System.out.println("\n下载链接列表:");
        for (int i = 0; i < downloadUrls.size(); i++) {
            System.out.println((i + 1) + ". " + downloadUrls.get(i));
        }
        
        // 检测版本类型
        String releaseType = extractReleaseType(jsonString);
        System.out.println("\n版本类型: " + releaseType);
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 主方法用于测试
     */
    public static void main(String[] args) {
        try {
            // 使用相对路径读取resources目录下的模板文件
            String jsonFilePath = "src/main/resources/githubApiTemple.json";
            testParseFromFile(jsonFilePath);
        } catch (IOException e) {
            System.err.println("文件读取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
