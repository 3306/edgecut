package com.edgecut.entity;

public class CutDataQTO {
    private String prefix;
    private String key;
    private Integer currentPage;
    private Integer pageSize;
    private Integer status;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getOffset(){
        if (currentPage == null){
            return null;
        }
        if (currentPage < 1){
            currentPage = 1;
        }
        return (currentPage - 1) * pageSize;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
