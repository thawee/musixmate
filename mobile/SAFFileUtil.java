package apincer.android.uamp.file;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by e1022387 on 6/19/2017.
 */

public class SAFFileUtil {

    /**
     * This error is thrown when the application does not have appropriate permission to write.<br><br>
     *
     * When this error is thrown the {@link SAFFileUtil} will attempt
     * to request permission causing the activity to break at that point.  Upon receiving a
     * successful result it will attempt to restart a saved method.<br><br>
     *
     */
    public class WritePermissionException extends IOException
    {
        public WritePermissionException(String message)
        {
            super(message);
        }
    }

    /**
     * Copy a file within the constraints of SAF.
     *
     * @param source
     *            The source uri
     * @param target
     *            The target uri
     * @return true if the copying was successful.
     * @throws WritePermissionException if the app does not have permission to write to target
     * @throws IOException if an I/O error occurs
     */
    public boolean copyFile(final Uri source, final Uri target)
            throws IOException
    {
        InputStream inStream = null;
        OutputStream outStream = null;

        UsefulDocumentFile destinationDoc = getDocumentFile(target, false, true);
        UsefulDocumentFile.FileData destinationData = destinationDoc.getData();
        if (!destinationData.exists)
        {
            destinationDoc.getParentFile().createFile(null, destinationData.name);
            // Note: destinationData is invalidated upon creation of the new file, so make a direct call following
        }
        if (!destinationDoc.exists())
        {
            throw new WritePermissionException(
                    "Write permission not found.  This indicates a SAF write permission was requested.  " +
                            "The app should store any parameters necessary to resume write here.");
        }

        try
        {
            inStream = FileUtil.getInputStream(this, source);
            outStream = getContentResolver().openOutputStream(target);

            Util.copy(inStream, outStream);
        }
        catch(ArithmeticException e)
        {
            Log.d(TAG, "File larger than 2GB copied.");
        }
        catch(Exception e)
        {
            throw new IOException("Failed to copy " + source.getPath() + ": " + e.toString());
        }
        finally
        {
            Util.closeSilently(inStream);
            Util.closeSilently(outStream);
        }
        return true;
    }

    /**
     * Copy a file within the constraints of SAF.
     *
     * @param source
     *            The source file
     * @param target
     *            The target file
     * @return true if the copying was successful.
     */
    public boolean copyFile(final File source, final File target)
            throws WritePermissionException
    {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (FileUtil.isWritable(target))
            {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            }
            else {
                if (Util.hasLollipop())
                {
                    // Storage Access Framework
                    UsefulDocumentFile targetDocument = getDocumentFile(target, false, true);
                    if (targetDocument == null)
                        return false;
                    outStream =
                            getContentResolver().openOutputStream(targetDocument.getUri());
                }
                else
                {
                    return false;
                }

                if (outStream != null) {
                    // Both for SAF and for Kitkat, write to output stream.
                    byte[] buffer = new byte[4096]; // MAGIC_NUMBER
                    int bytesRead;
                    while ((bytesRead = inStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                }

            }
        }
        catch (Exception e) {
            Log.e(TAG,
                    "Error when copying file to " + target.getAbsolutePath(), e);
            return false;
        }
        finally {
            Util.closeSilently(inStream);
            Util.closeSilently(outStream);
            Util.closeSilently(inChannel);
            Util.closeSilently(outChannel);
        }
        return true;
    }

    /**
     * Delete a file within the constraints of SAF.
     *
     * @param file the uri to be deleted.
     * @return True if successfully deleted.
     */
    public boolean deleteFile(final Uri file)
            throws WritePermissionException
    {
        if (FileUtil.isFileScheme(file))
        {
            return deleteFile(new File(file.getPath()));
        }
        else
        {
            UsefulDocumentFile document = getDocumentFile(file, false, true);
            return document != null && document.delete();
        }
    }

    /**
     * Delete a file within the constraints of SAF.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    private boolean deleteFile(final File file)
            throws WritePermissionException
    {
        if (!file.exists())
            return false;

        // First try the normal deletion.
        if (file.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Util.hasLollipop())
        {
            UsefulDocumentFile document = getDocumentFile(file, false, true);
            return document != null && document.delete();
        }

        return !file.exists();
    }

    /**
     * Move a file within the constraints of SAF.
     *
     * @param source The source uri
     * @param target The target uri
     * @return true if the copying was successful.
     * @throws WritePermissionException if the app does not have permission to write to target
     * @throws IOException if an I/O error occurs
     */
    public boolean moveFile(final Uri source, final Uri target) throws IOException
    {
        if (FileUtil.isFileScheme(target) && FileUtil.isFileScheme(target))
        {
            File from = new File(source.getPath());
            File to = new File(target.getPath());
            return moveFile(from, to);
        }
        else
        {
            boolean success = copyFile(source, target);
            if (success) {
                success = deleteFile(source);
            }
            return success;
        }
    }

    /**
     * Move a file within the constraints of SAF.
     *
     * @param source The source file
     * @param target The target file
     * @return true if the copying was successful.
     */
    public boolean moveFile(final File source, final File target) throws WritePermissionException
    {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }

        boolean success = copyFile(source, target);
        if (success) {
            success = deleteFile(source);
        }
        return success;
    }

    /**
     * Rename a folder within the constraints of SAF.
     *
     * @param source
     *            The source folder.
     * @param target
     *            The target folder.
     * @return true if the renaming was successful.
     */
    @SuppressWarnings("unused")
    public boolean renameFolder(final File source,
                                final File target)
            throws WritePermissionException
    {
        // First try the normal rename.
        if (source.renameTo(target)) {
            return true;
        }
        if (target.exists()) {
            return false;
        }

        // Try the Storage Access Framework if it is just a rename within the same parent folder.
        if (Util.hasLollipop() && source.getParent().equals(target.getParent())) {
            UsefulDocumentFile document = getDocumentFile(source, true, true);
            if (document == null)
                return false;
            if (document.renameTo(target.getName())) {
                return true;
            }
        }

        // Try the manual way, moving files individually.
        if (!mkdir(target)) {
            return false;
        }

        File[] sourceFiles = source.listFiles();

        if (sourceFiles == null) {
            return true;
        }

        for (File sourceFile : sourceFiles) {
            String fileName = sourceFile.getName();
            File targetFile = new File(target, fileName);
            if (!copyFile(sourceFile, targetFile)) {
                // stop on first error
                return false;
            }
        }
        // Only after successfully copying all files, delete files on source folder.
        for (File sourceFile : sourceFiles) {
            if (!deleteFile(sourceFile)) {
                // stop on first error
                return false;
            }
        }
        return true;
    }

    /**
     * Create a folder within the constraints of the SAF.
     *
     * @param folder
     *            The folder to be created.
     * @return True if creation was successful.
     */
    public boolean mkdir(final File folder)
            throws WritePermissionException
    {
        if (folder.exists()) {
            // nothing to create.
            return folder.isDirectory();
        }

        // Try the normal way
        if (folder.mkdir()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Util.hasLollipop()) {
            UsefulDocumentFile document = getDocumentFile(folder, true, true);
            if (document == null)
                return false;
            // getLollipopDocument implicitly creates the directory.
            return document.exists();
        }

        return false;
    }

    /**
     * Create a folder within the constraints of the SAF.
     *
     * @param folder
     *            The folder to be created.
     * @return True if creation was successful.
     */
    @SuppressWarnings("unused")
    public boolean mkdir(final Uri folder)
            throws WritePermissionException
    {
        UsefulDocumentFile document = getDocumentFile(folder, true, true);
        if (document == null)
            return false;
        // getLollipopDocument implicitly creates the directory.
        return document.exists();
    }

    /**
     * Delete a folder within the constraints of SAF
     *
     * @param folder
     *            The folder
     *
     * @return true if successful.
     */
    @SuppressWarnings("unused")
    public boolean rmdir(final File folder)
            throws WritePermissionException
    {
        if (!folder.exists()) {
            return true;
        }
        if (!folder.isDirectory()) {
            return false;
        }
        String[] fileList = folder.list();
        if (fileList != null && fileList.length > 0) {
            // Delete only empty folder.
            return false;
        }

        // Try the normal way
        if (folder.delete()) {
            return true;
        }

        // Try with Storage Access Framework.
        if (Util.hasLollipop())
        {
            UsefulDocumentFile document = getDocumentFile(folder, true, true);
            return document != null && document.delete();
        }

        return !folder.exists();
    }

    /**
     * Delete a folder within the constraints of SAF
     *
     * @param folder
     *            The folder
     *
     * @return true if successful.
     */
    @SuppressWarnings("unused")
    public boolean rmdir(final Uri folder)
            throws WritePermissionException
    {
        UsefulDocumentFile folderDoc = getDocumentFile(folder, true, true);
        UsefulDocumentFile.FileData file = folderDoc.getData();
        return !file.exists || file.isDirectory && folderDoc.listFiles().length <= 0 && folderDoc.delete();
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder
     *            the folder
     * @return true if successful.
     */
    @SuppressWarnings("unused")
    public boolean deleteFilesInFolder(final File folder)
            throws WritePermissionException
    {
        boolean totalSuccess = true;

        String[] children = folder.list();
        if (children != null) {
            for (String aChildren : children)
            {
                File file = new File(folder, aChildren);
                if (!file.isDirectory())
                {
                    boolean success = deleteFile(file);
                    if (!success)
                    {
                        Log.w(TAG, "Failed to delete file" + aChildren);
                        totalSuccess = false;
                    }
                }
            }
        }
        return totalSuccess;
    }

    /**
     * Delete all files in a folder.
     *
     * @param folder
     *            the folder
     * @return true if successful.
     */
    @SuppressWarnings("unused")
    public boolean deleteFilesInFolder(final Uri folder)
            throws WritePermissionException
    {
        boolean totalSuccess = true;
        UsefulDocumentFile folderDoc = getDocumentFile(folder, true, true);
        UsefulDocumentFile[] children = folderDoc.listFiles();
        for (UsefulDocumentFile child : children)
        {
            if (!child.isDirectory())
            {
                if (!child.delete())
                {
                    Log.w(TAG, "Failed to delete file" + child);
                    totalSuccess = false;
                }
            }
        }
        return totalSuccess;
    }

    /**
     * Get a list of external SD card paths. (Kitkat or higher.)
     *
     * @return A list of external SD card paths.
     */
    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected String[] getExtSdCardPaths() {
        List<String> paths = new ArrayList<>();
        for (File file : getExternalFilesDirs("external")) {
            if (file != null && !file.equals(getExternalFilesDir("external"))) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w(TAG, "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    }
                    catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths.toArray(new String[paths.size()]);
    }

    public UsefulDocumentFile getDocumentFile(final File file,
                                              final boolean isDirectory,
                                              final boolean createDirectories)
            throws WritePermissionException {
        return getDocumentFile(Uri.fromFile(file), isDirectory, createDirectories);
    }

    /**
     * Get a DocumentFile corresponding to the given file.  If the file does not exist, it is created.
     *
     * @param uri The file.
     * @param isDirectory flag indicating if the file should be a directory.
     * @param createDirectories flag indicating if intermediate path directories should be created if not existing.
     * @return The DocumentFile
     */
    public UsefulDocumentFile getDocumentFile(final Uri uri,
                                              final boolean isDirectory,
                                              final boolean createDirectories)
            throws WritePermissionException
    {
		/*
		The logic flow here may seem a bit odd, but the goal is to ensure minimal resolver calls
		Most file data calls to a DocumentFile involve a resolver, so we split the logic a bit
		to avoid additional .canWrite and .exists calls.
		 */
        UsefulDocumentFile targetDoc = UsefulDocumentFile.fromUri(this, uri);
        UsefulDocumentFile.FileData targetData = targetDoc.getData();

        if (targetData == null || !targetData.exists)   // TODO: I believe null will always supersede .exists, check
        {
            String name;
            if (targetData == null) //target likely doesn't exist, get the name
                name = targetDoc.getName();
            else
                name = targetData.name;

            UsefulDocumentFile parent = targetDoc.getParentFile();
            if (parent == null && !createDirectories)
                return null;

			/*
			This next part is necessary because DocumentFile.findFile is extremely slow in large
			folders, so what we do instead is track up the tree what folders need creation
			and place them in a stack (Using the convenient *working* UsefulDocumentFile.getParentFile).
			We then walk the stack back down creating folders as needed.
			*/

            Stack<UsefulDocumentFile> hierarchyTree = new Stack<>();
            // Create an hierarchical tree stack of folders that need creation
            // Stop if the parent exists or we've reached the root
            while (parent != null && !parent.exists())
            {
                hierarchyTree.push(parent);
                parent = parent.getParentFile();
            }

            // We should be at the top of the tree
            if (parent != null && !hierarchyTree.empty())
            {
                UsefulDocumentFile outerDirectory = parent;
                // Now work back down to create the directories if needed
                while (!hierarchyTree.empty())
                {
                    UsefulDocumentFile innerDirectory = hierarchyTree.pop();
                    outerDirectory = outerDirectory.createDirectory(innerDirectory.getName());
                    if (outerDirectory == null)
                    {
                        // TODO: Should we assume write permission is the issue here?
                        throw new WritePermissionException(
                                "Write permission not found.  This indicates a SAF write permission was requested.  " +
                                        "The app should store any parameters necessary to resume write here.");
                    }
                }
            }

            parent = targetDoc.getParentFile();

            if (isDirectory)
            {
                targetDoc = parent.createDirectory(name);
            }
            else
            {
                targetDoc = parent.createFile(null, name);
            }

            // If the target could not be created we don't have write permission. Possible other reasons?
            if (targetDoc == null)
            {
                throw new WritePermissionException(
                        "Write permission not found.  This indicates a SAF write permission was requested.  " +
                                "The app should store any parameters necessary to resume write here.");
            }
        }

        return targetDoc;
    }

}
