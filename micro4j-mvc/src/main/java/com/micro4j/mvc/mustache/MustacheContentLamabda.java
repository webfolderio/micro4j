package com.micro4j.mvc.mustache;

import java.io.IOException;
import java.io.Writer;

import com.samskivert.mustache.Mustache.Lambda;
import com.samskivert.mustache.Template.Fragment;

public class MustacheContentLamabda implements Lambda {

    private String content;

    public MustacheContentLamabda(String content) {
        this.content = content;
    }

    @Override
    public void execute(Fragment frag, Writer out) throws IOException {
        frag.execute(out);
        out.write(content);
    }
}
