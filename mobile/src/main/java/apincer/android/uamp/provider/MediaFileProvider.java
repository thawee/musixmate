package apincer.android.uamp.provider;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import apincer.android.provider.StorageProvider;
import apincer.android.uamp.BuildConfig;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.utils.FileUtils;

public class MediaFileProvider extends StorageProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".storage.documents";
    private static final String TAG = LogHelper.makeLogTag(MediaFileProvider.class);
    private static MediaFileProvider INSTANCE;
    public MediaFileProvider() {
        INSTANCE = this;
    }

    public static MediaFileProvider getInstance() {
        return INSTANCE;
    }

    public Uri getPersistedUri() {
        if(! getContext().getContentResolver().getPersistedUriPermissions().isEmpty()) {
            return getContext().getContentResolver().getPersistedUriPermissions().get(0).getUri();
        }
        return null;
    }

    /**
     * Returns the extension of the given file.
     * The extension is empty if there is no extension
     * The extension is the string after the last "."
     *
     * @param f The file whose extension is requested
     * @return The extension of the given file
     */
    public static String getExtension(final File f)
    {
        if(f==null) {
            return "";
        }
        return getExtension(f.getAbsolutePath());
    }

    public static String getExtension(final String f)
    {
        if(f==null) {
            return "";
        }
        final String name = f.toLowerCase();
        final int i = name.lastIndexOf(".");
        if (i == -1)
        {
            return "";
        }

        return name.substring(i + 1);
    }

    public static String removeExtension(String path) {
        if(path==null) {
            return "";
        }
        return removeExtension(new File(path));
    }

    public static String removeExtension(File file) {
        if(file==null) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public String getDisplayName(String path) {
        StorageProvider.RootInfo root = MediaFileProvider.getInstance().getRootInfo(path);
        if(root!=null) {
            String rootPath = root.path.getAbsolutePath();
            if(path.startsWith(rootPath)) {
                path = path.replace(rootPath, root.title+":/");
            }
        }
        return path;
    }

    public String getRootPath(String path) {
        StorageProvider.RootInfo root = MediaFileProvider.getInstance().getRootInfo(path);
        if(root!=null) {
            String rootPath = root.path.getAbsolutePath();
            if(path.startsWith(rootPath)) {
                return root.path.getAbsolutePath();
            }
        }
        return "";
    }

    public  DocumentFile getDocumentFile(final File file) {
        String documentId = null;
        try {
            documentId = getDocIdForFile(file);
            return FileUtils.getDocumentFile(getContext(),documentId,file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void safFromCache(File cacheFile, String path) {
        if(copy(cacheFile, new File(path)) ){
            delete(cacheFile);
        }
    }

    public  File safToCache(File file) {
        File cacheFile = new File(getContext().getFilesDir(), file.getName());
        if(copy(file, cacheFile)) {
            return cacheFile;
        }
        return null;
    }


    public void cleanEmptyDirectory(File directory) {
        if(directory !=null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if(files==null || files.length==0){
                delete(directory);
                // clean parent folder if empty
                if(directory!=null) {
                    cleanEmptyDirectory(directory.getParentFile());
                }
            }
        }
    }

    /*
     * file manipulations
     */
    public boolean copy(final File source, final File target) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inStream = new FileInputStream(source);

            // First try the normal way
            if (isWritable(target)) {
                // standard way
                outStream = new FileOutputStream(target);
                inChannel = inStream.getChannel();
                outChannel = ((FileOutputStream) outStream).getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } else {
                // Storage Access Framework
                /* old good
                Uri documentUri = getChildUri(getRelativePath(target.getAbsolutePath(),false));
                DocumentFile documentFile = DocumentFile.fromSingleUri(mContext,documentUri);
                if(!documentFile.exists()) {
                    createDocumentFile(target,false,mContext);
                }
                */
                DocumentFile documentFile = getDocumentFile(target);
                if(!documentFile.exists()) {
                    newDocumentFile(target);
                    //documentFile.createFile("image",target.getName());
                }
                Uri documentUri = documentFile.getUri();

                // open stream
                //ParcelFileDescriptor pfd = openDocument(getDocIdForFile(target),"rw",null);
                ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(documentUri, "w");
                if (pfd != null) {
                    outStream = new FileOutputStream(pfd.getFileDescriptor());
                    if (outStream != null) {
                        // Both for SAF and for Kitkat, write to output stream.
                        byte[] buffer = new byte[16384]; // MAGIC_NUMBER
                        int bytesRead;
                        while ((bytesRead = inStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, bytesRead);
                        }
                    }
                    pfd.close();
                }
            }
        } catch (Exception e) {
            LogHelper.e(TAG,
                    "Error when copying file from " + source.getAbsolutePath() + " to " + target.getAbsolutePath(), e);
            return false;
        } finally {
            closeSilently(inStream);
            closeSilently(outStream);
            closeSilently(inChannel);
            closeSilently(outChannel);
        }
        return true;
    }


    // Copy an InputStream to a File.
    public void copy(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            LogHelper.e(TAG, e);
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
            }
        }
    }

    public boolean move(String path, String newPath) {
        boolean success = false;
        File newFile = new File(newPath);
        File file = new File(path);

        // create new directory if not existed
        File newDir  = newFile.getParentFile();
        if(!newDir.exists()) {
            // create new directory
            mkdirs(newDir);
        }

        if(copy(file, newFile)) {
            success = delete(file);
        }else {
            delete(newFile);
        }
        return success;
    }

    /**
     * Delete a file within the constraints of SAF.
     *
     * @param file the file to be deleted.
     * @return True if successfully deleted.
     */
    public boolean delete(final File file) {
        if (!file.exists()) {
            LogHelper.i(TAG, "deleteFile", "cannot delete path "+file.getAbsolutePath());
            return false;
        }

        // First try the normal deletion.
        if (file.delete()) {
            LogHelper.i(TAG, "deleteFile", "delete path "+file.getAbsolutePath());
            return true;
        }

        // Try with Storage Access Framework.
        LogHelper.i(TAG, "deleteFile", "start deleting DocumentFile");
        DocumentFile documentFile = getDocumentFile(file);
        if(documentFile!=null && documentFile.exists()) {
            documentFile.delete();
        }
        /*
        if (Util.hasLollipop()) {
            LogHelper.i(TAG, "deleteFile", "start getting DocumentFile");
            String relativePath = getRelativePath(file.getAbsolutePath(),true);
            Uri permissionUri =getPersistedUri();
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(permissionUri,DocumentsContract.getTreeDocumentId(permissionUri));
            //Uri documentUri = getChildUri(docUri,relativePath);
            Uri documentUri = getDocumentUri(docUri,relativePath);

            LogHelper.i(TAG, "deleteFile", "start deleting DocumentFile");
            DocumentFile document = DocumentFile.fromSingleUri(mContext, documentUri);
            if(document!=null && document.exists()) {
                document.delete();
            }
        }*/

        LogHelper.i(TAG, "deleteFile", "check file is existed");
        return !file.exists();
    }

    public boolean newDocumentFile(File file) throws FileNotFoundException {
        String baseFolder = getRootPath(file.getAbsolutePath());

        if (baseFolder == null) {
            return false;
        }

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        } catch (IOException e) {
            return false;
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = getDocumentFile(file);

        String[] parts = relativePath.split("\\/");
        String newPath = "";
        for (int i = 0; i < parts.length; i++) {
            String displayName = StringUtils.trimToEmpty(parts[i]);
            newPath = newPath + "/" + displayName;
            DocumentFile nextDocument = document.findFile(displayName);
            if (nextDocument == null || (!nextDocument.exists())) {
                if ((i < parts.length - 1)) {
                   // nextDocument = document.createDirectory(displayName);
                   DocumentsContract.createDocument(getContext().getContentResolver(), document.getUri(), DocumentsContract.Document.MIME_TYPE_DIR, displayName);
                }
                else {
                   DocumentsContract.createDocument(getContext().getContentResolver(), document.getUri(), "image", displayName);
                   // nextDocument = document.createFile("image", displayName);
                }
                nextDocument = getDocumentFile(new File(baseFolder+"/"+newPath));
            }
            document = nextDocument;
        }
        return document.isFile();
    }

    public boolean mkdirs(File file) {
        if(file==null)
            return false;
        if (file.exists()) {
            // nothing to create.
            return file.isDirectory();
        }

        // Try the normal way
        if (file.mkdirs()) {
            return true;
        }

        // Try with Storage Access Framework.
        /*
        LogHelper.i(TAG, "mkdirs", "start create directory by DocumentFile");
        DocumentFile documentFile = getDocumentFile(file);
        if(!documentFile.exists()) {
            String[] parts = file.getAbsolutePath().split("\\/");
            //DocumentFile dirUri=null;
            String newPath = "";
            for (int i = 0; i < parts.length; i++) {
                String displayName = StringUtils.trimToEmpty(parts[i]);
                if (StringUtils.isEmpty(newPath)) {
                    newPath = displayName;
                } else {
                    newPath = newPath + "/" + displayName;
                }
                DocumentFile dirUri = getDocumentFile(new File(newPath));
                if(!dirUri.exists()) {
                    dirUri.createDirectory(displayName);
                }
            }
        }
        return documentFile.exists();
        */
        return false;
    }

    /**
     * Check if a file is writable. Detects write issues on external SD card.
     *
     * @param file The file
     * @return true if the file is writable.
     */
    public static boolean isWritable(final File file) {
        if (file == null)
            return false;
        boolean isExisting = file.exists();

        try {
            FileOutputStream output = new FileOutputStream(file, true);
            closeSilently(output);
        } catch (FileNotFoundException e) {
            return false;
        }
        boolean result = file.canWrite();

        // Ensure that file is not created during this process.
        if (!isExisting) {
            file.delete();
        }

        return result;
    }


    public static void saveImage(Bitmap resource, File coverartFile) {
        try {
            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            resource.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            byte[] bitmapdata = bos.toByteArray();

//write the bytes in file
            FileOutputStream fos = new FileOutputStream(coverartFile);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        }
        catch ( IOException e ) {
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException t) {
            LogHelper.w(TAG, "close fail ", t);
        }
    }
}
