package io.github.atengk.basic.model;

/**
 * 分页对象
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class Page {

    private int page;
    private int size;

    public Page(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
