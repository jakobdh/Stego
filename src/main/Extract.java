package main;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import james.JpegEncoder;
import ortega.HuffmanDecode;

public class Extract {
    private static String inFileName; // carrier file

    private static byte[] carrier; // carrier data

    private static int[] coeff; // dct values

    private static FileOutputStream fos; // embedded file (output file)

    private static String embFileName; // output file name

    private static String password;

    private static byte[] deZigZag = {
            0, 1, 5, 6, 14, 15, 27, 28, 2, 4, 7, 13, 16, 26, 29, 42, 3, 8, 12, 17, 25, 30, 41, 43, 9, 11, 18, 24, 31,
            40, 44, 53, 10, 19, 23, 32, 39, 45, 52, 54, 20, 22, 33, 38, 46, 51, 55, 60, 21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63 };

    public static void extract(Image image, final OutputStream fos, final String password)
            throws IOException {
    	JpegDecoder jpg = new JpegDecoder(image, 80);
        System.out.println("Huffman decoding starts");
        coeff = jpg.RetrieveCoefficients();
        ArrayList<Integer> read_coeffs = new ArrayList<Integer>();
        System.out.println("Permutation starts");
        System.out.println(coeff.length + " indices shuffled");
        int extractedByte = 0;
        int availableExtractedBits = 0;
        int extractedFileLength = 0;
        int nBytesExtracted = 0;
        int extractedBit;
        int i;
        // has to be the same value as used for the encoding
        int bit = 8;
        int ind = (int) (Math.log(bit) / Math.log(2));
        System.out.println("Extraction starts");
        // extract length information
        for (i = 0; availableExtractedBits < 32; i++) {
            if (i % 64 == 0) {
                continue; // skip DC coefficients
            }
            // j = i - i % 64 + deZigZag[i % 64];
            if (Math.abs(coeff[i]) < 2*bit) {
                continue; // skip zeroes
            }
            if (coeff[i] > 0) {
                extractedBit = (coeff[i] & bit) >> ind;
            } else {
                extractedBit = 1 - ((coeff[i] & bit) >> ind);
            }
            read_coeffs.add(i);
            extractedFileLength |= extractedBit << availableExtractedBits++;
        }
        extractedFileLength &= 0x007fffff;
        System.out.println("Length of embedded file: " + extractedFileLength + " bytes");
        availableExtractedBits = 0;
        
        System.out.println("Default code used");
        for (; i < coeff.length; i++) {
            if (i % 64 == 0) {
                continue; // skip DC coefficients
            }
            // j = i - i % 64 + deZigZag[i % 64];
            if (Math.abs(coeff[i]) < 2*bit) {
                continue; // skip zeroes
            }
            if (coeff[i] > 0) {
                extractedBit = (coeff[i] & bit) >> ind;
            } else {
                extractedBit = 1 - ((coeff[i] & bit) >> ind);
            }
            read_coeffs.add(i);
            extractedByte |= extractedBit << availableExtractedBits++;
            if (availableExtractedBits == 8) {
                // remove pseudo random pad
                fos.write((byte) extractedByte);
                extractedByte = 0;
                availableExtractedBits = 0;
                nBytesExtracted++;
                if (nBytesExtracted == extractedFileLength) {
                    break;
                }
            }
        }
        if (nBytesExtracted < extractedFileLength) {
            System.out.println("Incomplete file: only " + nBytesExtracted + " of " + extractedFileLength
                    + " bytes extracted");
        }
        try {
			PrintWriter writer = new PrintWriter("read_coeffs.txt", "UTF-8");
			writer.println(coeff.length + "coefficients found.");
			writer.println("List of all coefficients after facebook upload:");
			for (int coeffnt : coeff){
				writer.print(coeffnt + ", ");
			}
			writer.println();
			writer.println("Indices of coefficients that have contain hidden info:");
			for (int num : read_coeffs) {
				writer.print(num);
				writer.print(", ");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void main(final String[] args) {
        embFileName = "output.txt";
        password = "abc123";
        try {
            if (args.length < 1) {
                usage();
                return;
            }
            for (int i = 0; i < args.length; i++) {
                if (!args[i].startsWith("-")) {
                    if (!args[i].endsWith(".jpg")) {
                        usage();
                        return;
                    }
                    inFileName = args[i];
                    continue;
                }
                if (args.length < i + 1) {
                    System.out.println("Missing parameter for switch " + args[i]);
                    usage();
                    return;
                }
                if (args[i].equals("-e")) {
                    embFileName = args[i + 1];
                } else if (args[i].equals("-p")) {
                    password = args[i + 1];
                } else {
                    System.out.println("Unknown switch " + args[i] + " ignored.");
                }
                i++;
            }

            fos = new FileOutputStream(new File(embFileName));
            Image image = Toolkit.getDefaultToolkit().getImage(inFileName);
            extract(image, fos, password);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    static void usage() {
        System.out.println("java Extract [Options] \"image.jpg\"");
        System.out.println("Options:");
        System.out.println("\t-p password (default: abc123)");
        System.out.println("\t-e extractedFileName (default: output.txt)");
        System.out.println("\nAuthor: Andreas Westfeld, westfeld@inf.tu-dresden.de");
    }
}
