package apincer.android.uamp.provider;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apincer.android.provider.StorageProvider;
import apincer.android.uamp.utils.LogHelper;
import apincer.android.uamp.utils.StringUtils;
import apincer.android.uamp.utils.Util;
import apincer.android.utils.FileUtils;

/**
 * Created by e1022387 on 5/18/2017.
 */

public class AndroidFile {
  //  private static final String PATH_DOCUMENT = "document";
    private static final String PATH_TREE = "tree";

   // private static final String URL_SLASH = "%2F";
  //  private static final String URL_COLON = "%3A";
    private static final String TAG = LogHelper.makeLogTag(AndroidFile.class);
    //private static ConcurrentMap<String, String> storageDisplayNames = new ConcurrentHashMap<>();
    private static Map<String, String> storageDisplayNames = new HashMap<>();
    private Context mContext;
    private List<File> mAvailableSDCards = new ArrayList();

    public AndroidFile(Context mContext) {
        this.mContext = mContext;
        initAvailableSDCards();
    }

    @Deprecated
    public  boolean mkdirs(Context context, File file) {
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
       // DocumentFile document = createDocumentFile(file, true, context);
        DocumentFile document = createDocumentFile(file,context);
        // getDocumentFile implicitly creates the directory.
        if(document!=null) {
            return document.exists();
        }
        return false;
    }



/*
    public String getStorageName(Context context, String string) {
        String pathType ="N/A";
        if(string == null) {
            return pathType;
        }

        File files[] = getAllAvailableSDCards();
        if (files == null) {
            return pathType;
        }

        for (int i=0;i<files.length;i++) {
            String sdPath = files[i].getAbsolutePath();
            if (string.startsWith(sdPath)) {
                if(i==0) {
                    pathType = "MEM";
                }else {
                    pathType = "SD";
                }
                break;
            }
        }
        return pathType;
    } */

    public String getRelativePath(String fileLocation, boolean sdCardOnly) {
        if(fileLocation == null) {
            return "";
        }

        File files[] = getAllAvailableSDCards();
        if (files == null) {
            return fileLocation;
        }

        for (int i=0;i<files.length;i++) {
            String sdPath = files[i].getAbsolutePath();
            if (fileLocation.startsWith(sdPath)) {
                if(sdCardOnly && i==0) {
                    return fileLocation;
                }else {
                    return fileLocation.substring(sdPath.length()+1);
                }
            }
        }

        return fileLocation;
    }

    /**
     * Check if a file is readable.
     *
     * //@param file The file
     * @return true if the file is reabable.
     */
    /*
    public static boolean isReadable(final File file) {
        if (file == null)
            return false;
        if (!file.exists()) return false;

        boolean result;
        try {
            result = file.canRead();
        } catch (SecurityException e) {
            return false;
        }

        return result;
    }*/

    public static boolean isAvailable(String state) {
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private static File getAvailableRoot(File file) {
        if (file == null) {
            return null;
        }

        File root = file;
        while (isAvailable(Environment.getExternalStorageState(root.getParentFile()))) {
            root = root.getParentFile();
        }
        return root;
    }

    private boolean initAvailableSDCards() {
        File []files = mContext.getExternalCacheDirs();
        for (int i = 0; i < files.length; ++i) {
            files[i] = getAvailableRoot(files[i]);
            mAvailableSDCards.add(files[i]);
        }
        return true;
    }

    private File[] getAllAvailableSDCards() {
        return mAvailableSDCards.toArray(new File[0]);
    }

    private  String getExtSdCardFolder(final File file, Context context) {
        File files[] = getAllAvailableSDCards();
        if (files == null) {
            return null;
        }
        try {
            for (int i=0;i<files.length;i++) {
                String extSdPath = files[i].getAbsolutePath();
                if (file.getCanonicalPath().startsWith(extSdPath)) {
                    if(i>0) {
                        return extSdPath;
                    }
                    break;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    public  DocumentFile getDocumentFile(final File file, Context context) {
        StorageProvider storageProvider = new StorageProvider();
        ProviderInfo inf = new ProviderInfo();
        inf.authority = "";
        inf.grantUriPermissions = true;
        inf.exported = true;
        inf.readPermission = android.Manifest.permission.MANAGE_DOCUMENTS;
        inf.writePermission = android.Manifest.permission.MANAGE_DOCUMENTS;
        storageProvider.attachInfo(context, inf);
        storageProvider.onCreate();
        String documentId = null;
        try {
            documentId = storageProvider.getDocIdForFile(file);
            return FileUtils.getDocumentFile(context,documentId,file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public  DocumentFile createDocumentFile(final File file, Context context) {
        StorageProvider storageProvider = new StorageProvider();
        ProviderInfo inf = new ProviderInfo();
        inf.authority = "";
        inf.grantUriPermissions = true;
        inf.exported = true;
        inf.readPermission = android.Manifest.permission.MANAGE_DOCUMENTS;
        inf.writePermission = android.Manifest.permission.MANAGE_DOCUMENTS;
        storageProvider.attachInfo(context, inf);
        storageProvider.onCreate();
        String documentId = null;
        try {
            documentId = storageProvider.getDocIdForFileMaybeCreate(file,true);
            return FileUtils.getDocumentFile(context,documentId,file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public  DocumentFile createDocumentFile(final File file, boolean isDirectory, Context context) {
        Uri treeUri = getPersistedUri();
        String baseFolder = getExtSdCardFolder(file, context);

        if (baseFolder == null) {
            return null;
        }

        if (treeUri == null) {
            return null;
        }

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        } catch (IOException e) {
            return null;
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, treeUri);

        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);
            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory) {
                    nextDocument = document.createDirectory(parts[i]);
                }
                else {
                    nextDocument = document.createFile("image", parts[i]);
                }
            }
            document = nextDocument;
        }
        if (document.isFile()) {
            return document;
        }
        return null;
    }

    public static String getNameWithoutExtension(File file) {

        if(file==null) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    /*
    public  String getShortName(Context context, String string) {
        String storageName = getStorageName(context,string);
        String relativeName = getRelativePath(string,false);
        return storageName+"/"+relativeName;
    }*/
/*
    public String getDisplayPath(String path) {
        if(storageDisplayNames.isEmpty()) {
            File files[] = getAllAvailableSDCards();
            if (files == null) {
                return path;
            }

            for (int i=0;i<files.length;i++) {
                if(files[i]==null) continue;;
                String sdPath = files[i].getAbsolutePath();
                if(i==0) {
                    storageDisplayNames.put(sdPath, "INT");
                }else {
                    storageDisplayNames.put(sdPath, "SD");
                }
            }
        }
        for (String sdPath: storageDisplayNames.keySet()) {
            if(path.startsWith(sdPath)) {
                path = path.replace(sdPath, storageDisplayNames.get(sdPath)+":/");
                break;
            }
        }
        int dotIndex = path.lastIndexOf('.');
        return (dotIndex == -1) ? path : path.substring(0, dotIndex);
    }
*/

    public static String getFileExtention(String fileName) {
        if(fileName==null) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(dotIndex+1,fileName.length());
    }


    public  String getStoragePath(Context context, String path) {
        String pathType ="";
        if(path == null) {
            return pathType;
        }

        File files[] = getAllAvailableSDCards();
        if (files == null) {
            return pathType;
        }

        for (int i=0;i<files.length;i++) {
            String sdPath = files[i].getAbsolutePath();
            if (path.startsWith(sdPath)) {
                if(i==0) {
                    pathType = sdPath;
                }else {
                    pathType = sdPath;
                }
                break;
            }
        }
        return pathType;
    }

    /**********
     * Use UsefulDocumentFile
     *********/

    /**
     * Returns a uri to a child file within a folder.  This can be used to get an assumed uri
     * to a child within a folder.  This avoids heavy calls to DocumentFile.listFiles or
     * write-locked createFile
     *
     * This will only work with a uri that is an heriacrchical tree similar to SCHEME_FILE
     * @param hierarchicalTreeUri folder to install into
     * @param filename filename of child file
     * @return Uri to the child file
     */
    public static Uri getChildUri(Uri hierarchicalTreeUri, String filename) {
        String parentDocumentId = getTreeDocumentId(hierarchicalTreeUri);
        String childDocumentId = parentDocumentId + "/" + filename;
        return DocumentsContract.buildChildDocumentsUriUsingTree(
                hierarchicalTreeUri, childDocumentId);
    }

    public static Uri getDocumentUri(Uri hierarchicalTreeUri, String filename) {
        String parentDocumentId = getTreeDocumentId(hierarchicalTreeUri);
        String childDocumentId = parentDocumentId + "/" + filename;
        return DocumentsContract.buildDocumentUriUsingTree(
                hierarchicalTreeUri, childDocumentId);
    }

    /**
     * Returns a uri to a child file within a folder.  This can be used to get an assumed uri
     * to a child within a folder.  This avoids heavy calls to DocumentFile.listFiles or
     * write-locked createFile
     *
     * This will only work with a uri that is an heriacrchical tree similar to SCHEME_FILE
     * //@param filename filename of child file
     * @return Uri to the child file
     */
    /*
    public Uri getChildUri(String filename) {
        Uri hierarchicalTreeUri = getPersistedUri();
        return getChildUri(hierarchicalTreeUri, filename);
    }*/

    public Uri getPersistedUri() {
        if(!mContext.getContentResolver().getPersistedUriPermissions().isEmpty()) {
            return mContext.getContentResolver().getPersistedUriPermissions().get(0).getUri();
        }
        return null;
    }

    /**
     * Extract the via {@link DocumentsContract.Document#COLUMN_DOCUMENT_ID} from the given URI.
     * From {@link DocumentsContract} but return null instead of throw
     */
    public static String getTreeDocumentId(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) {
            return paths.get(1);
        }
        return null;
    }

    /*
    public static boolean isTreeUri(Uri uri) {
        final List<String> paths = uri.getPathSegments();
        return (paths.size() == 2 && PATH_TREE.equals(paths.get(0)));
    }*/

    /**
     * True if the uri has a tree segment
     */
    /*
    public static boolean hasTreeDocumentId(Uri uri) {
        return getTreeDocumentId(uri) != null;
    }*/

    /**
     * Extract the {@link DocumentsContract.Document#COLUMN_DOCUMENT_ID} from the given URI.
     * From {@link DocumentsContract} but return null instead of throw
     */
    /*
    @Nullable
    public static String getDocumentId(@NonNull Uri documentUri) {
        final List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && PATH_DOCUMENT.equals(paths.get(0))) {
            return paths.get(1);
        }
        if (paths.size() >= 4 && PATH_TREE.equals(paths.get(0))
                && PATH_DOCUMENT.equals(paths.get(2))) {
            return paths.get(3);
        }
        return null;
    }
*/
    public boolean mkdirs(Uri self, String path) {
        String[] parts = path.split("\\/");
        //DocumentFile dirUri=null;
        String newPath = "";
        for (int i = 0; i < parts.length; i++) {
            if(StringUtils.isEmpty(newPath)) {
                newPath = parts[i];
            }else {
                newPath = newPath+"/"+parts[i];
            }
            //Uri uri = getDocumentUri(self, newPath);
            Uri uri = getChildUri(self, newPath);
            DocumentFile dirUri = DocumentFile.fromSingleUri(mContext, uri);
            if(!dirUri.exists()) {
                createDirectory(self, parts[i]);
                //DocumentFile parentUri = DocumentFile.fromSingleUri(mContext, self);
                //parentUri.createDirectory(parts[i]);
                //dirUri.getParentFile().createDirectory(parts[i]);
                //DocumentsContract.createDocument(mContext.getContentResolver(), self, DocumentsContract.Document.MIME_TYPE_DIR, parts[i]);
            }
            self = uri;
        }
        return true; //(dirUri!=null && dirUri.exists());
    }

    /*
    public Uri createFile(Uri self,String displayName) {
        try {
            return DocumentsContract.createDocument(mContext.getContentResolver(), self, "image",displayName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    public Uri createDirectory(Uri self, String displayName) {
        try {
            return DocumentsContract.createDocument(mContext.getContentResolver(), self, DocumentsContract.Document.MIME_TYPE_DIR,displayName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


/*
    public DocumentFile getDocumentFile(File file) {
        Uri documentUri = getChildUri(getRelativePath(file.getAbsolutePath(),false));
        return DocumentFile.fromSingleUri(mContext,documentUri);
    }
*/
/*
    public DocumentFile getDocumentFile(Uri treeUri, String docId, boolean isDirectory) {
        DocumentFile documentFile = null;

        Uri uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
        if (isDirectory) {
            documentFile = DocumentFile.fromTreeUri(mContext, uri);
        } else {
            documentFile = DocumentFile.fromSingleUri(mContext, uri);
        }
        return documentFile;
    }
*/
    public static String getNameWithoutExtension(String path) {
        return getNameWithoutExtension(new File(path));
    }

}
