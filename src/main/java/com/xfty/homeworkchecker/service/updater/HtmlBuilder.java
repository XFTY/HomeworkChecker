package com.xfty.homeworkchecker.service.updater;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * HtmlBuilder — Markdown → HTML 转换服务
 * <p>
 * 使用 CommonMark 库将 Markdown 文本渲染为 HTML，
 * 并包裹深色主题样式，用于 WebView 中的更新日志展示。
 * </p>
 */
public class HtmlBuilder {
    
    private static final String DARK_BACKGROUND_COLOR = "#1e1e1e";
    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();
    
    /**
     * Convert markdown text to HTML with dark background
     * 
     * @param markdown The markdown text to convert
     * @return HTML string with dark background styling
     */
    public static String convertToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "<html><body style='background-color: " + DARK_BACKGROUND_COLOR + "; color: #d4d4d4;'></body></html>";
        }
        
        // Parse markdown and render to HTML
        var document = parser.parse(markdown);
        String htmlContent = renderer.render(document);
        
        // Wrap with HTML structure and dark theme styling
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html>\n");
        htmlBuilder.append("<html>\n<head>\n");
        htmlBuilder.append("<meta charset='UTF-8'>\n");
        htmlBuilder.append("<style>\n");
        htmlBuilder.append("body {\n");
        htmlBuilder.append("    background-color: ").append(DARK_BACKGROUND_COLOR).append(";\n");
        htmlBuilder.append("    color: #d4d4d4;\n");
        htmlBuilder.append("    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
        htmlBuilder.append("    padding: 20px;\n");
        htmlBuilder.append("    line-height: 1.6;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("a { color: #569cd6; }\n");
        htmlBuilder.append("code {\n");
        htmlBuilder.append("    background-color: #2d2d2d;\n");
        htmlBuilder.append("    padding: 2px 6px;\n");
        htmlBuilder.append("    border-radius: 3px;\n");
        htmlBuilder.append("    font-family: 'Consolas', 'Courier New', monospace;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("pre {\n");
        htmlBuilder.append("    background-color: #2d2d2d;\n");
        htmlBuilder.append("    padding: 15px;\n");
        htmlBuilder.append("    border-radius: 5px;\n");
        htmlBuilder.append("    overflow-x: auto;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("pre code {\n");
        htmlBuilder.append("    background-color: transparent;\n");
        htmlBuilder.append("    padding: 0;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("blockquote {\n");
        htmlBuilder.append("    border-left: 4px solid #569cd6;\n");
        htmlBuilder.append("    padding-left: 15px;\n");
        htmlBuilder.append("    margin-left: 0;\n");
        htmlBuilder.append("    color: #9cdcfe;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("table {\n");
        htmlBuilder.append("    border-collapse: collapse;\n");
        htmlBuilder.append("    width: 100%;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("th, td {\n");
        htmlBuilder.append("    border: 1px solid #3e3e3e;\n");
        htmlBuilder.append("    padding: 8px;\n");
        htmlBuilder.append("    text-align: left;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("th {\n");
        htmlBuilder.append("    background-color: #2d2d2d;\n");
        htmlBuilder.append("}\n");
        htmlBuilder.append("</style>\n");
        htmlBuilder.append("</head>\n<body>\n");
        htmlBuilder.append(htmlContent);
        htmlBuilder.append("</body>\n</html>");
        
        return htmlBuilder.toString();
    }
}
