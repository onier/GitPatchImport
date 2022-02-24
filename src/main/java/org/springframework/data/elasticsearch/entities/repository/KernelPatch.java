package org.springframework.data.elasticsearch.entities.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "kernel_patch", createIndex = true)
public class KernelPatch {
    @Id
    private String id;
    @Field(type = FieldType.Keyword)
    private String commitID;
    @Field(type = FieldType.Keyword)
    private String kernelName;
    @Field(type = FieldType.Keyword)
    private String branchName;
    @Field(type = FieldType.Text)
    private String acked;
    @Field(type = FieldType.Text)
    private String cc;
    @Field(type = FieldType.Integer)
    private int commitTime;
    @Field(type = FieldType.Text)
    private String detail;
    @Field(type = FieldType.Text)
    private String fixes;
    @Field(type = FieldType.Keyword)
    private String authorName;
    @Field(type = FieldType.Keyword)
    private String authorEmail;
    @Field(type = FieldType.Date)
    private Date commitDate;
    @Field(type = FieldType.Text)
    private String link;
    @Field(type = FieldType.Text)
    private String review;
    @Field(type = FieldType.Text)
    private String signed;
    @Field(type = FieldType.Text)
    private String subject;
    @Field(type = FieldType.Text)
    private String type;
    @Field(type = FieldType.Keyword)
    private String fileName;
    @Field(type = FieldType.Text)
    private String content;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommitID() {
        return commitID;
    }

    public void setCommitID(String commitID) {
        this.commitID = commitID;
    }

    public String getKernelName() {
        return kernelName;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAcked() {
        return acked;
    }

    public void setAcked(String acked) {
        this.acked = acked;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public int getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(int commitTime) {
        this.commitTime = commitTime;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getFixes() {
        return fixes;
    }

    public void setFixes(String fixes) {
        this.fixes = fixes;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(Date commitDate) {
        this.commitDate = commitDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getSigned() {
        return signed;
    }

    public void setSigned(String signed) {
        this.signed = signed;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
