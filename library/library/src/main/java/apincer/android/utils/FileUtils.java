package apincer.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import apincer.android.library.R;

import static android.provider.DocumentsContract.buildDocumentUri;
import static android.provider.DocumentsContract.getTreeDocumentId;

public class FileUtils {
    private static final String PATH_TREE = "tree";
    private static final String PATH_DOCUMENT = "document";
    public static final String BASIC_MIME_TYPE = "application/octet-stream";

    public static String getTypeForFile(File file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    private static String getTypeForFile(DocumentFile file) {
        if (file.isDirectory()) {
            return DocumentsContract.Document.MIME_TYPE_DIR;
        } else {
            return getTypeForName(file.getName());
        }
    }

    public static String formatFileCount(Context context, int count) {
        String value = NumberFormat.getInstance().format(count);
        String fileIndex = context.getString(R.string.index_file);
        String empty = context.getString(R.string.index_empty);
        return count == 0 ? empty : value + " " + fileIndex;
    }

    public static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return BASIC_MIME_TYPE;
    }

    private static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(treeUri.getAuthority()).appendPath(PATH_TREE)
                .appendPath(getTreeDocumentId(treeUri)).appendPath(PATH_DOCUMENT)
                .appendPath(documentId).build();
    }

    private static boolean isTreeUri(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        return (paths.size() >= 2 && PATH_TREE.equals(paths.get(0)));
    }

    public static Uri buildDocumentUriMaybeUsingTree(Uri baseUri, String documentId) {
        if (isTreeUri(baseUri)) {
            return buildDocumentUriUsingTree(baseUri, documentId);
        } else {
            return buildDocumentUri(baseUri.getAuthority(), documentId);
        }
    }

    public static Uri getRootUri(Context context, String docId){
        Uri treeUri = null;

        //get root dynamically
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            String treeRootId = getRootUri(permission.getUri());
            if(docId.startsWith(treeRootId)){
                treeUri = permission.getUri();
                return treeUri;
            }
        }
        return treeUri;
    }

    private static String getRootUri(Uri uri) {
        if (isTreeUri(uri)) {
            return DocumentsContract.getTreeDocumentId(uri);
        }
        return DocumentsContract.getDocumentId(uri);
    }

    /**
     * Test if a file lives under the given directory, either as a direct child
     * or a distant grandchild.
     * <p>
     * Both files <em>must</em> have been resolved using
     * {@link File#getCanonicalFile()} to avoid symlink or path traversal
     * attacks.
     */
    public static boolean contains(File dir, File file) {
        if (dir == null || file == null) return false;
        String dirPath = dir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (dirPath.equals(filePath)) {
            return true;
        }
        if (!dirPath.endsWith("/")) {
            dirPath += "/";
        }
        return filePath.startsWith(dirPath);
    }


    public static void updateMediaStore(Context context, String path) {
        try {
            Uri contentUri = Uri.fromFile(new File(path).getParentFile());
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            context.sendBroadcast(mediaScanIntent);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String makeFilePath(File parentFile, String name){
        if(null == parentFile || TextUtils.isEmpty(name)){
            return "";
        }
        return new File(parentFile, name).getPath();
    }

    public static int parseMode(String mode) {
        final int modeBits;
        if ("r".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE;
        } else if ("rwt".equals(mode)) {
            modeBits = ParcelFileDescriptor.MODE_READ_WRITE
                    | ParcelFileDescriptor.MODE_CREATE
                    | ParcelFileDescriptor.MODE_TRUNCATE;
        } else {
            throw new IllegalArgumentException("Bad mode '" + mode + "'");
        }
        return modeBits;
    }
}