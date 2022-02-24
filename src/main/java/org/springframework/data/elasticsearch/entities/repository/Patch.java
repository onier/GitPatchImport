package org.springframework.data.elasticsearch.entities.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Patch {
    private String commitID;
    private String from;
    private String date;
    private String subject;
    private String detail;
    private String fixes;
    private String signed;
    private String asked;
    private String Cc;
    private String link;
    private String reviewed;
    private String filePath;
    private String diff;
    private String patchPath;
    private String kernelName;

    private String authorName;
    private String authorEmail;
    private long commitDate;
    private int commitTime;
    private String type;

    public String getAuthorName() {
        return authorName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(long commitDate) {
        this.commitDate = commitDate;
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


    public int getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(int commitTime) {
        this.commitTime = commitTime;
    }


    public String getPatchPath() {
        return patchPath;
    }

    public void setPatchPath(String patchPath) {
        this.patchPath = patchPath;
    }

    public String getKernelName() {
        return kernelName;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    public interface Parser {
        String parse(String text);
    }

    public static Vector<Patch> createPatchs(String director) throws Exception {
        Vector<Patch> result = new Vector<>();
        File file = new File(director);
        File[] fs = file.listFiles();
        for (File f : fs) {
            if (f.isFile())
                result.addAll(getPaths(f.getAbsolutePath()));
        }
        return result;
    }

    public static Patch create(Map<String, String> fields) throws Exception {
        Patch patch = new Patch();
        patch.commitID = fields.get("From ").trim();
        patch.from = fields.get("From: ").trim();
        SimpleDateFormat sdf = new SimpleDateFormat("E, d MMM yyyy HH:mm:23 Z", Locale.ENGLISH);
        patch.date = fields.get("Date: ").trim();
        patch.subject = fields.get("Subject: ").trim();
        patch.fixes = fields.get("Fixes: ").trim();
        patch.signed = fields.get("Signed-off-by: ").trim();
        patch.asked = fields.get("Acked-by: ").trim();
        patch.Cc = fields.get("Cc: ").trim();
        patch.link = fields.get("Link: ").trim();
        patch.reviewed = fields.get("Reviewed-by: ").trim();
        patch.filePath = fields.get("diff --git a").trim();
        return patch;
    }

    public static Vector<Patch> getPaths(String path) throws Exception {
        Map<String, String> fields = new HashMap<>();
        fields.put("From ", "");
        fields.put("From: ", "");
        fields.put("Date: ", "");
        fields.put("Subject: ", "");
        fields.put("Fixes: ", "");
        fields.put("Signed-off-by: ", "");
        fields.put("Acked-by: ", "");
        fields.put("Cc: ", "");
        fields.put("Link: ", "");
        fields.put("Reviewed-by: ", "");
        fields.put("diff --git a", "");

        Map<String, Parser> parses = new HashMap<>();
        parses.put("From ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("From ");
                return (values[1].split(" ")[0]);
            }
        });
        parses.put("From: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("From: ");
                return values[1];
            }
        });
        parses.put("Date: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Date: ");
                return values[1];
            }
        });
        parses.put("Subject: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Subject: ");
                return values[1];
            }
        });
        parses.put("Fixes: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Fixes:");
                return values[1];
            }
        });
        parses.put("Signed-off-by: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Signed-off-by: ");
                return values[1];
            }
        });
        parses.put("Acked-by: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Acked-by: ");
                return values[1];
            }
        });
        parses.put("Cc: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Cc: ");
                return values[1];
            }
        });
        parses.put("Link: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Link: ");
                return values[1];
            }
        });
        parses.put("Reviewed-by: ", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("Reviewed-by: ");
                return values[1];
            }
        });
        parses.put("diff --git a", new Parser() {
            @Override
            public String parse(String text) {
                String[] values = text.split("diff --git a");
                return (values[1].split(" ")[0]);
            }
        });

        Vector<Patch> result = new Vector<Patch>();
        List<String> contents = FileUtils.readLines(new File(path), Charset.defaultCharset());
        String lastKey = "", detail = "", diff = "";
        boolean detailFinish = false, diffFinish = false;

        for (int i = 0; i < contents.size(); i++) {
            String line = contents.get(i);
            boolean matchTag = false;
            for (Map.Entry<String, Parser> entry : parses.entrySet()) {
                if (line.startsWith(entry.getKey())) {
                    matchTag = true;
                    lastKey = entry.getKey();
                    if (lastKey.equals("Subject: ")) {
                        fields.put(lastKey, fields.get(lastKey) + " " + entry.getValue().parse(line));
                        for (int j = i + 1; j < contents.size(); j++) {
                            if (!contents.get(j).trim().isEmpty()) {
                                fields.put(lastKey, fields.get(lastKey) + contents.get(j));
                            } else {
                                i = j;
                                break;
                            }
                        }
                    } else {
                        if (fields.get(lastKey).trim().isEmpty()) {
                            fields.put(lastKey, fields.get(lastKey) + " " + entry.getValue().parse(line));
                        } else {
                            fields.put(lastKey, fields.get(lastKey) + ";" + entry.getValue().parse(line));
                        }
                    }
                    break;
                }
            }
            if (line.trim().equals("---") && !diffFinish) {
                detailFinish = true;
                diffFinish = true;
                for (int j = i; j < contents.size(); j++) {
                    diff = diff + contents.get(j) + "\n";
                }
            }
//            if (!detailFinish) {
//                if (fields.get(lastKey).trim().isEmpty()) {
//                    fields.put(lastKey, fields.get(lastKey) + entry.getValue().parse(line));
//                } else {
//                    fields.put(lastKey, fields.get(lastKey) + ";" + entry.getValue().parse(line));
//                }
//            }

            if (!detailFinish && !matchTag)
                detail = detail + "\n" + line;
        }
        Patch p = create(fields);
        p.detail = detail;
        p.diff = diff;
        p.patchPath = path;
        result.add(p);
        return result;
    }

    public String getCommitID() {
        return commitID;
    }

    public void setCommitID(String commitID) {
        this.commitID = commitID;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
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

    public String getSigned() {
        return signed;
    }

    public void setSigned(String signed) {
        this.signed = signed;
    }

    public String getAsked() {
        return asked;
    }

    public void setAsked(String asked) {
        this.asked = asked;
    }

    public String getCc() {
        return Cc;
    }

    public void setCc(String cc) {
        Cc = cc;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getReviewed() {
        return reviewed;
    }

    public void setReviewed(String reviewed) {
        this.reviewed = reviewed;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }
}
