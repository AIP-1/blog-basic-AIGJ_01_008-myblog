package com.example.blog.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        // 사용자가 쓴 글이므로 raw HTML 은 이스케이프해서 XSS 를 막는다
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .sanitizeUrls(true)
                .build();
    }

    public String toHtml(String markdown) {
        return renderer.render(parser.parse(markdown));
    }

    /** 목록 썸네일용: 본문의 첫 번째 이미지 주소 (내 서버 /uploads 이미지 또는 https 이미지만) */
    public String firstImage(String markdown) {
        String[] found = new String[1];
        parser.parse(markdown).accept(new AbstractVisitor() {
            @Override
            public void visit(Image image) {
                String url = image.getDestination();
                if (found[0] == null && url != null && (url.startsWith("/uploads/") || url.startsWith("https://"))) {
                    found[0] = url;
                }
            }
        });
        return found[0];
    }

    /** 공유 미리보기용 요약: 본문 문단의 글자만 모은 앞부분 (제목·코드·표·목록 제외) */
    public String summary(String markdown, int maxLength) {
        StringBuilder text = new StringBuilder();
        for (Node block = parser.parse(markdown).getFirstChild(); block != null; block = block.getNext()) {
            if (block instanceof Paragraph || block instanceof BlockQuote) {
                block.accept(new AbstractVisitor() {
                    @Override
                    public void visit(Text node) {
                        text.append(node.getLiteral());
                    }

                    @Override
                    public void visit(Code node) {
                        text.append(node.getLiteral());
                    }

                    @Override
                    public void visit(SoftLineBreak node) {
                        text.append(' ');
                    }
                });
                text.append(' ');
            }
        }
        String result = text.toString().replaceAll("\\s+", " ").trim();
        return result.length() <= maxLength ? result : result.substring(0, maxLength).trim() + "…";
    }
}
