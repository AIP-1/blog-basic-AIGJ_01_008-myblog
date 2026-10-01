package com.example.blog.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final TextContentRenderer textRenderer;

    public MarkdownService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        // 사용자가 쓴 글이므로 raw HTML 은 이스케이프해서 XSS 를 막는다
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .sanitizeUrls(true)
                .build();
        this.textRenderer = TextContentRenderer.builder().extensions(extensions).build();
    }

    public String toHtml(String markdown) {
        return renderer.render(parser.parse(markdown));
    }

    /** 공유 미리보기용 요약: 코드 블록을 빼고 마크다운 기호를 없앤 앞부분 */
    public String summary(String markdown, int maxLength) {
        Node document = parser.parse(markdown);
        Node node = document.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
                node.unlink();
            }
            node = next;
        }
        String text = textRenderer.render(document).replaceAll("\\s+", " ").trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength).trim() + "…";
    }
}
