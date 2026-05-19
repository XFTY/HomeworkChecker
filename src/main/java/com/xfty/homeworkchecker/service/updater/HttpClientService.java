package com.xfty.homeworkchecker.service.updater;

import com.xfty.homeworkchecker.Idf;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * HttpClientService — GitHub API 请求服务
 * <p>
 * 通过 OkHttp 向 GitHub Releases API 发起请求，获取最新 Release 的 JSON 数据。
 * 测试模式下加载本地 githubApiTemple.json 替代网络请求。
 * 解析工作委托给 JSONParsingService。
 * </p>
 */
public class HttpClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientService.class);
    private static final String GITHUB_API_URL = "https://api.github.com/repos/XFTY/Homeworkchecker/releases/latest";
    
    private OkHttpClient httpClient;
    private String jsonString;
    private String tagName;
    private String releaseTitle;
    private String releaseBody;
    private List<String> downloadUrls;
    private String releaseType;
    
    public HttpClientService() {
        // 创建OkHttpClient实例，设置超时时间
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 从GitHub API获取最新的Release信息并解析
     * @return 是否成功获取并解析
     */
    public boolean fetchAndParseLatestRelease() {
        try {
            String jsonResponse;
            
            // 检查是否处于测试模式
            if (Idf.UpdaterTestMode) {
                logger.info("Updater test mode enabled, loading from local template file");
                jsonResponse = loadLocalTemplateFile();
                if (jsonResponse == null) {
                    logger.error("Failed to load local template file");
                    return false;
                }
            } else {
                // 正常模式：从GitHub API获取
                logger.info("Fetching release information from GitHub API");
                jsonResponse = fetchFromGitHubApi();
                if (jsonResponse == null) {
                    return false;
                }
            }
            
            this.jsonString = jsonResponse;
            
            // 使用JSONParsingService解析JSON
            this.tagName = JSONParsingService.extractTagName(jsonString);
            this.releaseTitle = JSONParsingService.extractReleaseTitle(jsonString);
            this.releaseBody = JSONParsingService.extractReleaseBody(jsonString);
            this.downloadUrls = JSONParsingService.extractDownloadUrls(jsonString);
            this.releaseType = JSONParsingService.extractReleaseType(jsonString);
            
            logger.info("Successfully parsed release information: {}", releaseTitle);
            return true;
        } catch (Exception e) {
            logger.error("Failed to fetch and parse release information: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从本地模板文件加载JSON内容
     * @return JSON字符串，失败返回null
     */
    private String loadLocalTemplateFile() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("githubApiTemple.json")) {
            if (inputStream == null) {
                logger.error("Template file not found in resources");
                return null;
            }
            
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read template file: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从GitHub API获取JSON数据
     * @return JSON字符串，失败返回null
     */
    private String fetchFromGitHubApi() {
        Request request = new Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                logger.error("HTTP request failed with status code: {}", response.code());
                return null;
            }
        } catch (IOException e) {
            logger.error("Failed to fetch from GitHub API: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取Tag名称
     * @return tag名称，例如 "1.6.0"
     */
    public String getTagName() {
        return tagName;
    }
    
    /**
     * 获取发行版标题
     * @return 发行版标题，例如 "1.6.0 - Beta"
     */
    public String getReleaseTitle() {
        return releaseTitle;
    }
    
    /**
     * 获取发行版描述
     * @return 发行版描述内容
     */
    public String getReleaseBody() {
        return releaseBody;
    }
    
    /**
     * 获取所有下载链接
     * @return 下载链接列表
     */
    public List<String> getDownloadUrls() {
        return downloadUrls;
    }
    
    /**
     * 获取版本类型
     * @return 版本类型标签："Beta"、"Alpha"、"Snapshot" 或 "release"
     */
    public String getReleaseType() {
        return releaseType;
    }
    
    /**
     * 获取原始JSON字符串
     * @return 原始JSON字符串
     */
    public String getJsonString() {
        return jsonString;
    }
    
    /**
     * 测试方法：获取并打印最新的Release信息
     */
    public static void testFetchLatestRelease() {
        logger.info("=== Testing GitHub Release Fetch ===");
        
        HttpClientService service = new HttpClientService();
        boolean success = service.fetchAndParseLatestRelease();
        
        if (success) {
            logger.info("Tag Name: {}", service.getTagName());
            logger.info("Release Title: {}", service.getReleaseTitle());
            logger.info("Release Type: {}", service.getReleaseType());
            logger.info("Release Body (first 200 chars): {}", 
                service.getReleaseBody() != null && service.getReleaseBody().length() > 200 
                    ? service.getReleaseBody().substring(0, 200) + "..." 
                    : service.getReleaseBody());
            
            List<String> urls = service.getDownloadUrls();
            logger.info("Download URLs count: {}", urls.size());
            for (int i = 0; i < urls.size(); i++) {
                logger.info("  {}. {}", i + 1, urls.get(i));
            }
        } else {
            logger.error("Failed to fetch release information");
        }
        
        logger.info("=== Test Completed ===");
    }
    
    /**
     * 主方法用于测试
     */
    public static void main(String[] args) {
        testFetchLatestRelease();
    }
}
