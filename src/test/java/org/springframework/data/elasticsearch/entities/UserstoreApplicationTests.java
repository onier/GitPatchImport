package org.springframework.data.elasticsearch.entities;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FooterKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.elasticsearch.action.ActionRequestValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.entities.repository.Diff;
import org.springframework.data.elasticsearch.entities.repository.KernelPatch;
import org.springframework.data.elasticsearch.entities.repository.Patch;
import org.springframework.data.elasticsearch.entities.repository.KernelPatchReposiroty;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserstoreApplicationTests {

    @Autowired
    KernelPatchReposiroty kernelPatchReposiroty;
    Connection c = null;

    PreparedStatement statement, createPatchTableStatement, insertPatchStatement, createDiffTableStatement, insertDiffStatement;

    ExecutorService executor = Executors.newFixedThreadPool(8);
    private AtomicInteger count = new AtomicInteger();
    String patchCreateSql = "create table if not exists TABLE_NAME (\n" +
            "    asked text,\n" +
            "    cc text,\n" +
            "    commitID text,\n" +
            "    commitTime int,\n" +
            "    defail text,\n" +
            "    fixes  text,\n" +
            "    authorName text,\n" +
            "    authorEmail text,\n" +
            "    commitDate INTEGER,\n" +
            "    link  text,\n" +
            "    review text,\n" +
            "    signed text,\n" +
            "    subject text\n" +
            ")";

    String insertPatchSql = "insert into TABLE_NAME (asked, cc, commitID, commitTime, defail, fixes, authorName, authorEmail, commitDate, link, review, signed, subject)\n" +
            "values (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    String createDiffTableSql = "create table if not exists TABLE_NAME(\n" +
            "    commitID text,\n" +
            "    type text,\n" +
            "    fileName text,\n" +
            "    content text\n" +
            "    )";

    String insertDiffSql = "insert into TABLE_NAME(commitID, type, fileName, content) \n" +
            "values (?,?,?,?)";

    public UserstoreApplicationTests() throws Exception {
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:/home/xuzhenhai/kernerl_git.db3");
        c.setAutoCommit(false);
    }

    @Test
    public void testPatch() throws Exception {
//        String file = "/home/xuzhenhai/kernel/anolis/patch/patch/4931-khugepaged-collapse_pte_mapped_thp-protect-the-pmd-l.patch";
//        Vector<Patch> patchs = Patch.getPaths(file);
//        System.out.println(patchs.size());

        String director = "/home/xuzhenhai/kernel/anolis/CJlinux-4.19.90/patchs";
        File file = new File(director);
        File[] fs = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("patch");
            }
        });
        Map<Integer, File> caches = new TreeMap<>();
        List<Integer> indexs = new ArrayList<>();
        for (int i = 0; i < fs.length; i++) {
            int n = fs[i].getName().indexOf("-");
            indexs.add(Integer.parseInt(fs[i].getName().substring(0, n)));
            caches.put(Integer.parseInt(fs[i].getName().substring(0, n)), fs[i]);
        }
        Collections.sort(indexs);
        for (int i = 0; i < indexs.size(); i++) {
            File f = caches.get(indexs.get(i));
            Vector<Patch> ps;
            if (f.isFile()) {
                ps = Patch.getPaths(f.getAbsolutePath());
                for (Patch p : ps) {
//                    patchReposiroty.save(p);
                }
            }
            double d = (i + 1) * 1.0 / fs.length;
        }
    }

    Vector<Patch> getPathsFromDirector(String pathDir, String kernelname, String tableName) throws Exception {
        Vector<Patch> patches = new Vector<>();
        File file = new File(pathDir);
        File[] fs = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith("patch");
            }
        });
        Map<Integer, File> caches = new TreeMap<>();
        List<Integer> indexs = new ArrayList<>();
        for (int i = 0; i < fs.length; i++) {
            int n = fs[i].getName().indexOf("-");
            indexs.add(Integer.parseInt(fs[i].getName().substring(0, n)));
            caches.put(Integer.parseInt(fs[i].getName().substring(0, n)), fs[i]);
        }
        Collections.sort(indexs);
        for (int i = 0; i < indexs.size(); i++) {

            File f = caches.get(indexs.get(i));
            System.out.println(i + "  " + f.getName());
            Vector<Patch> ps = new Vector<>();
            if (f.isFile()) {
                ps = Patch.getPaths(f.getAbsolutePath());
                for (Patch p : ps) {
                    p.setKernelName(kernelname);
                    saveToSqlite(p, tableName);
                    count.incrementAndGet();
//                            patches.add(p);
                }
            }
        }
        while (count.get() != indexs.size()) {
            Thread.sleep(10000);
            System.out.println(patches.size());
        }
        return patches;
    }

    void saveToSqlite(Patch patche, String tableName) throws Exception {
        if (statement == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:/home/xuzhenhai/kernel_patch.db");
                String sql = "insert into " + tableName + " (kernel_name,commitID,'from','date',subject,detail,fixes,signed,asked,Cc,link,reviewed,filePath,diff,patchPath)\n" +
                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                statement = c.prepareStatement(sql);
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }

        }
        statement.setString(1, patche.getKernelName());
        statement.setString(2, patche.getCommitID());
        statement.setString(3, patche.getFrom());
        statement.setString(4, patche.getDate());
        statement.setString(5, patche.getSubject());
        statement.setString(6, patche.getDetail());
        statement.setString(7, patche.getFixes());
        statement.setString(8, patche.getSigned());
        statement.setString(9, patche.getAsked());
        statement.setString(10, patche.getCc());
        statement.setString(11, patche.getLink());
        statement.setString(12, patche.getReviewed());
        statement.setString(13, patche.getFilePath());
        statement.setString(14, patche.getDiff());
        statement.setString(15, patche.getPatchPath());
        if (!statement.execute()) {
        }
    }

    @Test
    public void createCJKenrel() throws Exception {
//        String cloudKernelPaths = "/home/xuzhenhai/kernel/anolis/CJlinux-4.19.90/cj_linux_patch";
//        String cjKernelPaths = "";
//        getPathsFromDirector(cloudKernelPaths,"CJLinux","cj_linux");
//        System.out.println(123);
        String cloudKernelPaths = "/home/xuzhenhai/kernel/anolis/cloud-kernel/anolis_kernel_patchs";
        getPathsFromDirector(cloudKernelPaths, "cloud_kernel", "cloud_kernel");
        System.out.println(123);
    }

    public void saveGitOneLine(List<String> lines, String tableName) throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:/home/xuzhenhai/kernel_patch.db");
            c.setAutoCommit(false);
            String sql = "insert into " + tableName + " (\"commitID\", subject,'order') values (?,?,?)";
            statement = c.prepareStatement(sql);
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        for (int i = 0; i < lines.size(); i++) {
            statement.setString(1, lines.get(i).substring(0, 12));
            statement.setString(2, lines.get(i).substring(12, lines.get(i).length()).trim());
            statement.setInt(3, i + 1);
            statement.addBatch();
        }
        statement.executeBatch();
        c.commit();

        statement.close();
        c.close();

    }

    @Test
    public void importGitLogOneline() throws Exception {
        {
            List<String> contents = FileUtils.readLines(new File("/home/xuzhenhai/kernel/anolis/CJlinux-4.19.90/cj_linux.log"), Charset.defaultCharset());
            saveGitOneLine(contents, "cj_linux_oneline");
        }
        {
            List<String> contents = FileUtils.readLines(new File("/home/xuzhenhai/kernel/anolis/cloud-kernel/cloud_kernel.log"), Charset.defaultCharset());
            saveGitOneLine(contents, "cloud_kernel_oneline");
        }
    }

    @Test
    public void testDate() throws ParseException {
        //Wed, 5 Aug 2020 12:58:23 -0600
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        System.out.println(sdf.format(new Date()));
        String str = "Fri, 17 Dec 2021 10:09:00 +0800";//""Wed, 5 Aug 2020 12:58:23 -0600";
        SimpleDateFormat sdf = new SimpleDateFormat("E, d MMM yyyy HH:mm:23 Z", Locale.ENGLISH);
        System.out.println(sdf.parse(str));
        System.out.println(sdf.format(new Date()));
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(repository.resolve(objectId));
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        treeParser.reset(reader, tree.getId());
        walk.dispose();
        return treeParser;
    }

    private static List<Diff> listDiff(Repository repository, Git git, String oldCommit, String newCommit) throws Exception {
        List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldCommit))
                .setNewTree(prepareTreeParser(repository, newCommit))
                .call();
        List<Diff> pathDiffs = new ArrayList<>();
//        System.out.println("Found: " + diffs.size() + " differences");
        for (DiffEntry diff : diffs) {
            Diff d = new Diff();
            d.setId(newCommit);
            d.setType(diff.getChangeType().toString());
            d.setFileName((diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
//            System.out.println("Diff: " + diff.getChangeType() + ": " +
//                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DiffFormatter formatter = new DiffFormatter(byteArrayOutputStream);
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
            formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));
            formatter.setDetectRenames(true);
//            formatter.getRenameDetector().setRenameScore(50);
            formatter.format(diff);

            String diffPatchString = byteArrayOutputStream.toString();

//            System.out.println("** printing commit DIFF:");
//            System.out.println(diffPatchString);
            d.setContent(diffPatchString);
            pathDiffs.add(d);
        }
        return pathDiffs;
    }

    public List<KernelPatch> getKernelPatchs(Patch patch, List<Diff> ds, String branchName, String kernel) {
        List<KernelPatch> rs = new ArrayList<>();
        for (int i = 0; i < ds.size(); i++) {
            KernelPatch kernelPatch = new KernelPatch();
            kernelPatch.setAcked(patch.getAsked());
            kernelPatch.setBranchName(branchName);
            kernelPatch.setKernelName(kernel);
            kernelPatch.setAuthorEmail(patch.getAuthorEmail());
            kernelPatch.setAuthorName(patch.getAuthorName());
            kernelPatch.setCc(patch.getCc());
            kernelPatch.setDetail(patch.getDetail());
            kernelPatch.setCommitID(patch.getCommitID());
            kernelPatch.setCommitTime(patch.getCommitTime());
            kernelPatch.setFixes(patch.getFixes());
            kernelPatch.setCommitDate(new Date(patch.getCommitDate()));
            kernelPatch.setLink(patch.getLink());
            kernelPatch.setReview(patch.getReviewed());
            kernelPatch.setSigned(patch.getSigned());
            kernelPatch.setSubject(patch.getSubject());
            kernelPatch.setType(ds.get(i).getType());
            kernelPatch.setFileName(ds.get(i).getFileName());
            kernelPatch.setContent(ds.get(i).getContent());
            rs.add(kernelPatch);
        }
        return rs;
    }
    File file = new File("log");
    File file1 = new File("errorlog");

    private static RevCommit listDiff(Repository repository, Git git, Iterator<RevCommit> iterator, String newCommit,List<Diff> pathDiffs) throws Exception {
        if(iterator.hasNext()) {
            RevCommit oldCommit = iterator.next();
            List<DiffEntry> diffs = git.diff()
                    .setOldTree(prepareTreeParser(repository, oldCommit.getName()))
                    .setNewTree(prepareTreeParser(repository, newCommit))
                    .call();
//        System.out.println("Found: " + diffs.size() + " differences");
            for (DiffEntry diff : diffs) {
                Diff d = new Diff();
                d.setId(newCommit);
                d.setType(diff.getChangeType().toString());
                d.setFileName((diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
//            System.out.println("Diff: " + diff.getChangeType() + ": " +
//                    (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DiffFormatter formatter = new DiffFormatter(byteArrayOutputStream);
                formatter.setRepository(repository);
                formatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
                formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));
                formatter.setDetectRenames(true);
//            formatter.getRenameDetector().setRenameScore(50);
                formatter.format(diff);

                String diffPatchString = byteArrayOutputStream.toString();

//            System.out.println("** printing commit DIFF:");
//            System.out.println(diffPatchString);
                d.setContent(diffPatchString);
                pathDiffs.add(d);
                return oldCommit;
            }
        }
        return null;
    }

    public void exportBranch(Repository repo, Git git, String branchName, String kernelName) throws Exception {
        checkTableExist(branchName);
        Iterable<RevCommit> log = git.log().add(repo.resolve(branchName)).call();
        RevWalk revWalk = new RevWalk(repo);
        List<Patch> patches = new ArrayList<>();
        List<Diff> diffList = new ArrayList<>();
        List<RevCommit> commits = new ArrayList<>();
        for (Iterator<RevCommit> iterator = log.iterator(); iterator.hasNext(); ) {
            commits.add(iterator.next());
        }

        for (int i =0;i<commits.size()-1;i++) {
            System.out.println(""+i+" "+commits.size());
            RevCommit rev = commits.get(i);
            List<Diff> rs = listDiff(repo, git, commits.get(i+1).getName(), rev.getName());
            Patch patch = new Patch();
            patch.setAsked(Arrays.toString(rev.getFooterLines("Acked-by").toArray()));
            patch.setCc(Arrays.toString(rev.getFooterLines("Cc").toArray()));
            patch.setCommitID(rev.getName());
            patch.setCommitTime(rev.getCommitTime());
            patch.setDetail(rev.getFullMessage());
            patch.setFixes(Arrays.toString(rev.getFooterLines(new FooterKey("Fixes")).toArray()));
            patch.setAuthorName(rev.getAuthorIdent().getName());
            patch.setAuthorEmail(rev.getAuthorIdent().getEmailAddress());
            patch.setCommitDate(rev.getAuthorIdent().getWhen().getTime());
            patch.setLink(Arrays.toString(rev.getFooterLines("Link").toArray()));
            patch.setReviewed(Arrays.toString(rev.getFooterLines("Reviewed-by").toArray()));
            patch.setSigned(Arrays.toString(rev.getFooterLines("Signed-off-by").toArray()));
            patch.setSubject(rev.getShortMessage());
//            patches.add(patch);
//            diffList.addAll(rs);
            List<KernelPatch> kps = getKernelPatchs(patch, rs, branchName, kernelName);
            FileUtils.writeByteArrayToFile(file,(patch.getCommitID()+"\n").getBytes());
            try {
                kernelPatchReposiroty.saveAll(kps);
            }catch (ActionRequestValidationException ex){
                FileUtils.writeByteArrayToFile(file,(patch.getCommitID()+"\n").getBytes());
            }
            catch(Throwable t){
                FileUtils.writeByteArrayToFile(file,(patch.getCommitID()+"\n").getBytes());
                }
            if (patches.size() > 1000 || diffList.size() > 3000) {
                savePatchs(patches, branchName);
                saveDiffs(diffList, branchName);
                patches.clear();
                diffList.clear();
            }
        }
    }

    public void checkTableExist(String branch) throws Exception {
        branch = branch.replaceAll("-", "_");
        branch = branch.replaceAll("\\.", "_");
        String patchTableName = branch.replaceAll("/", "_") + "_patchs";
        String sql = patchCreateSql.replaceFirst("TABLE_NAME", patchTableName);
        createPatchTableStatement = c.prepareStatement(sql);
        createPatchTableStatement.execute();

        sql = insertPatchSql.replaceFirst("TABLE_NAME", patchTableName);
        insertPatchStatement = c.prepareStatement(sql);

        String diffTableName = branch.replaceAll("/", "_") + "_diffs";
        sql = createDiffTableSql.replaceFirst("TABLE_NAME", diffTableName);
        createDiffTableStatement = c.prepareStatement(sql);
        createDiffTableStatement.execute();

        sql = insertDiffSql.replaceFirst("TABLE_NAME", diffTableName);
        insertDiffStatement = c.prepareStatement(sql);
        c.commit();
    }

    public void saveDiffs(List<Diff> diffs, String diffTableName) throws SQLException {
        for (int i = 0; i < diffs.size(); i++) {
            insertDiffStatement.setString(1, diffs.get(i).getId());
            insertDiffStatement.setString(2, diffs.get(i).getType());
            insertDiffStatement.setString(3, diffs.get(i).getFileName());
            insertDiffStatement.setString(4, diffs.get(i).getContent());
            insertDiffStatement.addBatch();
        }
        insertDiffStatement.executeBatch();
        c.commit();
    }

    public void savePatchs(List<Patch> patches, String tableName) throws SQLException {
        for (int i = 0; i < patches.size(); i++) {
            insertPatchStatement.setString(1, patches.get(i).getAsked());
            insertPatchStatement.setString(2, patches.get(i).getCc());
            insertPatchStatement.setString(3, patches.get(i).getCommitID());
            insertPatchStatement.setInt(4, patches.get(i).getCommitTime());
            insertPatchStatement.setString(5, patches.get(i).getDetail());
            insertPatchStatement.setString(6, patches.get(i).getFixes());
            insertPatchStatement.setString(7, patches.get(i).getAuthorName());
            insertPatchStatement.setString(8, patches.get(i).getAuthorEmail());
            insertPatchStatement.setLong(9, patches.get(i).getCommitDate());
            insertPatchStatement.setString(10, patches.get(i).getLink());
            insertPatchStatement.setString(11, patches.get(i).getReviewed());
            insertPatchStatement.setString(12, patches.get(i).getSigned());
            insertPatchStatement.setString(13, patches.get(i).getSubject());
            insertPatchStatement.addBatch();
        }
        insertPatchStatement.executeBatch();
        c.commit();
    }

    @Test
    public void testGit() throws Exception {
        Repository gitRepo = null;
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitFile = new File("/home/xuzhenhai/kernel/anolis/cloud-kernel/.git");
        Repository repo = builder.setGitDir(gitFile).setMustExist(true).build();
        Git git = new Git(repo);
        List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        for (Ref ref : refs) {
            exportBranch(repo, git, "refs/heads/release-4.19", "cloud_kernel");
            break;
        }
//        RevWalk revWalk = new RevWalk(repo);
////            listDiff(repo, git, "a0a26bb3e123", "95bc973a4e25");
////            return;
//        Iterable<RevCommit> log = git.log().call();
//        List<Patch> patches = new ArrayList<>();
//        List<Diff> diffList = new ArrayList<>();
//        int n = 0;
//        for (Iterator<RevCommit> iterator = log.iterator(); iterator.hasNext(); n++) {
//            if (n > 10) {
//                break;
//            }
//            RevCommit rev = iterator.next();
//            RevCommit[] parents = rev.getParents();
//            final RevCommit parent = revWalk.parseCommit(parents[0].getId());
////                System.out.println(rev.getName());
//            List<Diff> rs = listDiff(repo, git, parent.getName(), rev.getName());
//            diffList.addAll(rs);
//            Patch patch = new Patch();
//            patch.setAsked(Arrays.toString(rev.getFooterLines("Acked-by").toArray()));
//            patch.setCc(Arrays.toString(rev.getFooterLines("Cc").toArray()));
//            patch.setCommitID(rev.getName());
//            patch.setCommitTime(rev.getCommitTime());
//            patch.setDetail(rev.getFullMessage());
//            patch.setFixes(Arrays.toString(rev.getFooterLines(new FooterKey("Fixes")).toArray()));
//            patch.setAuthorName(rev.getAuthorIdent().getName());
//            patch.setAuthorEmail(rev.getAuthorIdent().getEmailAddress());
//            patch.setCommitDate(rev.getAuthorIdent().getWhen().getTime());
//            patch.setLink(Arrays.toString(rev.getFooterLines("Link").toArray()));
//            patch.setReviewed(Arrays.toString(rev.getFooterLines("Reviewed-by").toArray()));
//            patch.setSigned(Arrays.toString(rev.getFooterLines("Signed-off-by").toArray()));
//            patch.setSubject(rev.getShortMessage());
//            patches.add(patch);
//        }


    }

}
