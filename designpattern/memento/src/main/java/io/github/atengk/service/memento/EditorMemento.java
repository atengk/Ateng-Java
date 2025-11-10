package io.github.atengk.service.memento;

/**
 * 备忘录类，保存编辑器状态
 */
public class EditorMemento {

    private final String content;

    public EditorMemento(String content) {
        this.content = content;
    }

    /**
     * 获取保存的内容
     */
    public String getContent() {
        return content;
    }
}
