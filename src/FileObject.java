import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/***
 * Utility class to access, read and write pieces to file.
 * The associated file should have been created before the use of this class object.
 * */
public class FileObject {
    private RandomAccessFile fileAccess;
    private File file;
    private int fileSize;
    private int pieceSize;
    private String fileName;

    FileObject(String fileName, int fileSize, int pieceSize) throws FileNotFoundException {
        try {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.pieceSize = pieceSize;
            //System.out.println(fileName);
            file = new File(fileName);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
            fileAccess = new RandomAccessFile(file, "rw");
//            //System.out.println(fileAccess.length());
        } catch (FileNotFoundException ex) {
//            //System.out.println("File " + fileName + " does not exists!!");
            throw new FileNotFoundException();
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public void cleanUp(){
        try {
            fileAccess.close();
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
    /**
     * Read the piece with pieceIndex from the file.
     */
    public byte[] readPiece(int pieceIndex, int size) {
        byte[] piece = new byte[size];
        try {
            int offSet = (pieceIndex - 1) * pieceSize;
            fileAccess.seek(offSet);
            fileAccess.read(piece, 0, size);
//            //System.out.println(offSet  + " " + size + " " + piece.length);
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                fileAccess.close();
            } catch (IOException ex1) {
            }
        }
        return piece;
    }

    /***
     * Write piece to the file.
     **/
    public boolean writePiece(int pieceIndex, byte[] piece, int size) {
        try {
            int offSet = (pieceIndex - 1) * pieceSize;
            fileAccess.seek(offSet);
            fileAccess.write(piece, 0, size);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            try {
                fileAccess.close();
            } catch (IOException ex1) {
            }
        }
        return false;
    }
}
