package io.github.atengk.service.memento;

import org.springframework.stereotype.Component;

/**
 * 编辑器（发起人）
 */
@Component
public class Editor {

    private String content = "";

    /**
     * 写入内容
     */
    public void write(String text) {
        content += text;
        System.out.println("【编辑器】当前内容：" + content);
    }

    /**
     * 创建备忘录
     */
    public EditorMemento save() {
        return new EditorMemento(content);
    }

    /**
     * 恢复状态
     */
    public void restore(EditorMemento memento) {
        content = memento.getContent();
        System.out.println("【编辑器】已恢复内容：" + content);
    }

    public String getContent() {
        return content;
    }
}
