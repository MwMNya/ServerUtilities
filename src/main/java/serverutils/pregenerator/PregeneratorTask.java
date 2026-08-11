package serverutils.pregenerator;

public class PregeneratorTask {

    public final int dimension;

    private final int centerX;
    private final int centerZ;
    private final int radius;


    private int x;
    private int z;


    public PregeneratorTask(
            int dimension,
            int centerX,
            int centerZ,
            int radius){

        this.dimension = dimension;
        this.centerX=centerX;
        this.centerZ=centerZ;
        this.radius=radius;


        this.x=-radius;
        this.z=-radius;
    }



    public int nextX(){
        return centerX+x;
    }


    public int nextZ(){
        return centerZ+z;
    }



    public void advance(){

        z++;

        if(z>radius){
            z=-radius;
            x++;
        }

    }


    public boolean finished(){

        return x>radius;
    }

}