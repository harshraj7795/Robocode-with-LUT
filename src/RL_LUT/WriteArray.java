package RL_LUT;
import java.io.FileWriter;
import java.io.IOException;

public class WriteArray {
    public void writeArrayToTxt(double[][] data, String string) {
        int rowNum = data.length;
        int columnNum = data[0].length;
        try {
            FileWriter fw = new FileWriter(string);
            for (int i = 0; i < rowNum; i++) {
                for (int j = 0; j < columnNum; j++)
                    fw.write(data[i][j]+ "\t");
                fw.write("\n");
            }
            fw.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    public void writeArrayToExcel(double[][] data, String string) {
        int rowNum = data.length;
        int columnNum = data[0].length;
        try {
            FileWriter fw = new FileWriter(string);
            for (int i = 0; i < rowNum; i++) {
                for (int j = 0; j < columnNum; j++)
                    fw.write(data[i][j]+ "\t");
                fw.write("\n");
            }
            fw.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}

