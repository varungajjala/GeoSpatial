package geospatial.operation7;

import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.util.Vector;

 
//class that defines a recangle with double co-ordinates
class Rectangle
{
    double x;
    double y;
    double width;
    double height;
      
    Rectangle(double x1,double y1,double wid, double h)
    {
        x = x1;
        y = y1;
        width = wid;
        height = h;
          
    }
      
    //function to check if one rectangle contains another rectangle
    boolean contains(Rectangle r)
    {
        if((x <= r.x) && (y >= r.y) && ((x + width) >= (r.x + r.width)) && ((y - height) <= (r.y - r.height)))
        {
            return true;   
        }
        return false;
    }
}
  
public class App
{
      
    public static void main(String[] args)
    {
        //creating the spark configuration
    	SparkConf conf = new SparkConf().setAppName("App").setMaster("spark://192.168.0.4:7077").set("spark.eventLog.enabled","true").set("spark.executor.memory", "2g").set("spark.driver.memory", "2g");
        
    	final JavaSparkContext spark = new JavaSparkContext(conf);
        spark.addJar("/home/nagarjuna/DDS_Project_Team17/Programs/operation7.jar");  
        
        
        //Reading a file containing a set of points
        //JavaRDD<String> ip1 = spark.textFile("hdfs://master:54310/team17/operation6/JoinQueyTestData1.csv");
        //JavaRDD<String> ip1 = spark.textFile("hdfs://master:54310/team17/arealm.csv");
        JavaRDD<String> ip1 = spark.textFile("hdfs://master:54310/team17/areawater.csv");
        
    	//Reading a file containing a set of polygons
        //JavaRDD<String> ip2 = spark.textFile("hdfs://master:54310/team17/operation6/JoinQueyTestData2.csv");
        //JavaRDD<String> ip2 = spark.textFile("hdfs://master:54310/team17/arealmjoin.csv");
        JavaRDD<String> ip2 = spark.textFile("hdfs://master:54310/team17/areawaterjoin.csv");
        
    	//map function that returns the co-ordinates of first set of points
        JavaRDD<Vector> points = ip1.map(new Function<String,Vector>()
            {
                public Vector call(String line)
                {
                    String[] splits = line.split(",");
                    double[] data = new double[4];
                    data[0] = Double.parseDouble(splits[0]);
                    data[1] = Double.parseDouble(splits[1]);
                    data[2] = Double.parseDouble(splits[2]);  
                    data[3] = Double.parseDouble(splits[3]);
                    Vector v1 = new Vector(data);
                    return v1;
                }
            }
        );
          
        //map function that returns the co-ordinates of second set of polygons
        JavaRDD<Vector> points1 = ip2.map(new Function<String,Vector>()
            {
                public Vector call(String line)
                {
                    String[] splits = line.split(",");
                    double[] data = new double[4];
                    data[0] = Double.parseDouble(splits[0]);
                    data[1] = Double.parseDouble(splits[1]);
                    data[2] = Double.parseDouble(splits[2]);  
                    data[3] = Double.parseDouble(splits[3]);
                    Vector v1 = new Vector(data);
                    return v1;
                }
            }
        );
  
        List<Vector> var = points.collect();
         
        //Broadcasting the second set of points 
        final Broadcast<List<Vector>> var_broadcast=spark.broadcast(var);
  
        //function that returns all the polygon co-ordinates if one polygon is inside another polygon 
        JavaRDD<String> final_result = points1.map(new Function<Vector,String>()
            {
                public String call(Vector v1)
                {
                	int count=0;
                    double [] d1       = v1.elements();
                    double rec1_x      = Math.min(d1[0], d1[2]);
                    double rec1_y      = Math.max(d1[1], d1[3]);
                    double rec1_wid    = Math.abs(d1[2]-d1[0]);
                    double rec1_height = Math.abs(d1[3]-d1[1]);
  
                    String s = d1[0] + "," + d1[1] + "," + d1[2] + "," + d1[3];
                     
                    List<Vector> list = var_broadcast.value();
  
                    for(Vector ele:list)
                    {
                        double [] d2       = ele.elements();
                        double rec2_x      = Math.min(d2[0], d2[2]);
                        double rec2_y      = Math.max(d2[1], d2[3]);
                        double rec2_wid    = Math.abs(d2[2]-d2[0]);
                        double rec2_height = Math.abs(d2[3]-d2[1]);
                        Rectangle r1=new Rectangle(rec1_x,rec1_y,rec1_wid,rec1_height);
                        Rectangle r2=new Rectangle(rec2_x,rec2_y,rec2_wid,rec2_height);
                        if(r1.contains(r2))
                        {
                            count++;
                        }
                    }
                    System.out.println(s + "count ="+ count);
                    return s+" "+count;
                }
            }
        );
      
        //writing the final output to HDFS
        final_result.repartition(1).saveAsTextFile("hdfs://master:54310/team17/operation6/Output7_ld.txt");
    }
}
