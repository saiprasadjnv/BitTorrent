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

    FileObject(String fileName, int fileSize, int pieceSize) throws FileNotFoundException{
        try {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.pieceSize = pieceSize;
            file = new File(fileName);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        }catch (FileNotFoundException ex){
            System.out.println("File " + fileName + " does not exists!!");
            throw new FileNotFoundException();
        }
    }

    /**
     * Read the file piece with pieceIndex.
     * */
    public byte[] readPiece(int pieceIndex){
        byte[] piece = new byte[pieceSize];
        try {
            int offSet = (pieceIndex - 1) * pieceSize;
            fileAccess.readFully(piece, offSet, pieceSize);
        }catch(IOException ex){
            ex.printStackTrace();
        }finally {
            try {
                fileAccess.close();
            }catch (IOException ex){

            }
        }
        return piece;
    }

    /***
     * Write file piece to the file.
     **/
    public void writePiece(int pieceIndex, byte[] piece){
        try{
            int offSet = (pieceIndex-1)*pieceSize;
            fileAccess.write(piece, offSet, pieceSize);
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            try{
                fileAccess.close();
            }catch (IOException ex){

            }
        }
    }
}
