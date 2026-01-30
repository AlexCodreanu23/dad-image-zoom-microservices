package rmi;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.imageio.ImageIO;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {

    public ZoomServiceImpl() throws RemoteException { 
        super(); 
    }

    @Override
    public byte[] zoom(byte[] bmpBytes, String zoomType, int percent) throws RemoteException {
        System.out.println("C04 RMI zoom called: bytes=" + (bmpBytes == null ? 0 : bmpBytes.length)
                + " zoomType=" + zoomType + " percent=" + percent);
        
        if (bmpBytes == null || bmpBytes.length == 0) {
            return bmpBytes;
        }

        try {
            // Read BMP image
            ByteArrayInputStream bais = new ByteArrayInputStream(bmpBytes);
            BufferedImage originalImage = ImageIO.read(bais);
            
            if (originalImage == null) {
                System.err.println("C04: Failed to read image");
                return bmpBytes;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            // Calculate new dimensions
            double scale;
            if ("in".equals(zoomType)) {
                // Zoom in = make bigger
                scale = 1.0 + (percent / 100.0);
            } else {
                // Zoom out = make smaller
                scale = 1.0 / (1.0 + (percent / 100.0));
            }
            
            int newWidth = (int) (originalWidth * scale);
            int newHeight = (int) (originalHeight * scale);
            
            // Ensure minimum size
            newWidth = Math.max(1, newWidth);
            newHeight = Math.max(1, newHeight);
            
            System.out.println("C04: Resizing from " + originalWidth + "x" + originalHeight 
                    + " to " + newWidth + "x" + newHeight + " (scale=" + scale + ")");

            // Create resized image
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            // Write back to BMP bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "bmp", baos);
            byte[] result = baos.toByteArray();
            
            System.out.println("C04: Zoom complete, result bytes=" + result.length);
            return result;

        } catch (Exception e) {
            System.err.println("C04: Error during zoom: " + e.getMessage());
            e.printStackTrace();
            return bmpBytes;
        }
    }
}