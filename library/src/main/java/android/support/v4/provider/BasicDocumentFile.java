package android.support.v4.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.FileNotFoundException;

import apincer.android.utils.Utils;

public class BasicDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    BasicDocumentFile(DocumentFile parent, Context context, Uri uri) {
        super(parent);
        mContext = context;
        mUri = uri;
    }

    @Override
    public DocumentFile createFile(String mimeType, String displayName) {
        //final Uri result = apincer.android.provider.DocumentsContractApi21.createFile(mContext, mUri, mimeType, displayName);
        Uri result = null;
        try {
            result = DocumentsContract.createDocument(mContext.getContentResolver(), mUri, mimeType, displayName);
        } catch (FileNotFoundException fnf) {}
        return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
    }

    @Override
    public DocumentFile createDirectory(String displayName) {
        //final Uri result = apincer.android.provider.DocumentsContractApi21.createDirectory(mContext, mUri, displayName);
       //return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
        return createFile(DocumentsContract.Document.MIME_TYPE_DIR, displayName);
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public String getName() {
        return apincer.android.provider.DocumentsContractApi19.getName(mContext, mUri);
    }

    @Override
    public String getType() {
        return apincer.android.provider.DocumentsContractApi19.getType(mContext, mUri);
    }

    @Override
    public boolean isDirectory() {
        return apincer.android.provider.DocumentsContractApi19.isDirectory(mContext, mUri);
    }

    @Override
    public boolean isFile() {
        return apincer.android.provider.DocumentsContractApi19.isFile(mContext, mUri);
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public long lastModified() {
        return apincer.android.provider.DocumentsContractApi19.lastModified(mContext, mUri);
    }

    @Override
    public long length() {
        return apincer.android.provider.DocumentsContractApi19.length(mContext, mUri);
    }

    @Override
    public boolean canRead() {
        return apincer.android.provider.DocumentsContractApi19.canRead(mContext, mUri);
    }

    @Override
    public boolean canWrite() {
        return apincer.android.provider.DocumentsContractApi19.canWrite(mContext, mUri);
    }

    @Override
    public boolean delete() {
        return apincer.android.provider.DocumentsContractApi19.delete(mContext, mUri);
    }

    @Override
    public boolean exists() {
        return apincer.android.provider.DocumentsContractApi19.exists(mContext, mUri);
    }

    @Override
    public DocumentFile[] listFiles() {
        final Uri[] result = apincer.android.provider.DocumentsContractApi21.listFiles(mContext, mUri);
        final DocumentFile[] resultFiles = new DocumentFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new TreeDocumentFile(this, mContext, result[i]);
        }
        return resultFiles;
    }

    @Override
    public boolean renameTo(String displayName) {
        final Uri result = apincer.android.provider.DocumentsContractApi21.renameTo(mContext, mUri, displayName);
        if (result != null) {
            mUri = result;
            return true;
        } else {
            return false;
        }
    }

    public static DocumentFile fromUri(Context context, Uri treeUri) {
        if (Utils.hasLollipop()) {
            return new BasicDocumentFile(null, context, treeUri);
        } else {
            return new android.support.v4.provider.BasicStorageDocumentFile(null, context, treeUri);
        }
    }

}