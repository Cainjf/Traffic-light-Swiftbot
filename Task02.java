import swiftbot.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileWriter;

public class Task02 {
	static SwiftBotAPI swiftbot;
	static String trafficLightColor, frequentLight;
	static final int[] redLight = {255, 0, 0};
	static final int[] greenLight = {0, 0, 255};
	static final int[] blueLight = {0, 255, 0};
	static final int[] yellowLight = {255, 0, 255};
	static int no_trafficLights = 0, no_frequentLight = 0, decider = 0;
	static ArrayList<String> colorList = new ArrayList<String>();
	static long begin, end, time;

	
	public static void main(String[] args) throws InterruptedException {
		try {
            swiftbot = new SwiftBotAPI();
        } catch (Exception e) {
            
            System.out.println("\nI2C disabled!");
            System.out.println("Run the following command:");
            System.out.println("sudo raspi-config nonint do_i2c 0\n");
            System.exit(5);
        }
		
		System.out.println("Hello! Press button 'A' to begin the program.");
		swiftbot.enableButton(Button.A, () -> {
			System.out.println("Status: Program inititated \nTo stop the program, press button 'X'.");
			System.out.println("");
			
			while (true) {
				begin = System.currentTimeMillis();
				//Exits the program whenever Button X is pressed.
				swiftbot.enableButton(Button.X, () -> {
					end = System.currentTimeMillis();
					swiftbot.disableButton(Button.X);
					swiftbot.startMove(0, 0);
					System.out.println("Status: Program halted");
					System.out.println("Do you want to view the execution log? Press button 'Y' for yes or button 'X' for no.");
					
					swiftbot.enableButton(Button.Y, () -> {
						decider = 1;
						System.out.println("Status: Program finished");
						try {
							executionLog();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
					});
					
					swiftbot.enableButton(Button.X, () -> {
						System.out.println("Status: Program finished");
						decider = 2;
						try {
							executionLog();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
					});
				
		        });
				
				forwardMovement();
				
				try {
					//Detects an object and proceeds with checking the colour.
					trafficLightDetection();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        });
		
	}
	
	public static void forwardMovement() {
		swiftbot.fillUnderlights(yellowLight);
		swiftbot.startMove(45, 45);
	}
	
	//Detects object in front of the SwiftBot to perform required functions.
	public static void trafficLightDetection() throws InterruptedException {
	    double distanceToObject = 0;
	    while (true) {
	    	swiftbot.startMove(45, 45);
	    	distanceToObject = swiftbot.useUltrasound();
	    	//Checks if the object is within 20cm of the robot and stops 
	    	//it for the following function to be executed.
		    if (distanceToObject <= 20) {
		    	swiftbot.startMove(0, 0);
		    	no_trafficLights ++;
		    	checkColor();
		    } else {
		    	continue;
		    }
	    }
    }
	
	//Checks the traffic colour using RGB values.
	public static void checkColor() throws InterruptedException {
		BufferedImage img = swiftbot.takeStill(ImageSize.SQUARE_1080x1080);
		
		int sum = 0;
		long sumRed = 0, sumGreen = 0, sumBlue = 0,  red = 0, green = 0, blue = 0; 
		//Gets the RGB value for each pixel of the image.
		for(int w = 0; w < img.getWidth(); w++) {
			for(int h = 0; h < img.getHeight(); h++) {

				Color pixel = new Color(img.getRGB(w, h));
				red +=  pixel.getRed();
				green += pixel.getGreen();
				blue += pixel.getBlue();
				sum ++;
			}
		}
		//Calculates the sum of the red, green and blue colour values 
		//and sets the total amount to appropriate variables.
		sumRed = red/sum;
		sumGreen = green/sum;
		sumBlue = blue/sum;
		//Determines what colour is the traffic light based on the highest colour value.
		if (sumRed > sumGreen) {
			if (sumRed > sumBlue) {
				trafficLightColor = "Red";
			} else {
				trafficLightColor = "Blue";
			}
		}else {
			if (sumGreen > sumBlue) {
				trafficLightColor = "Green";
			} else {
				trafficLightColor = "Blue";
			}
		}
		colorList.add(trafficLightColor);
		//Performs appropriate action for each traffic light when encountered.
		action();
	}
	
	//Determines what action needs to be performed based on the value of trafficLightColor.
	public static void action() throws InterruptedException {
		if (trafficLightColor.equals("Green")) {
			greenAction();
		} else if (trafficLightColor.equals("Red")) {
			redAction();
		} else if (trafficLightColor.equals("Blue")) {
			blueAction();
		}
	}
	//Green light action
	public static void greenAction() throws InterruptedException {
		swiftbot.fillUnderlights(greenLight);
		swiftbot.move(80, 80, 2000);
		Thread.sleep(500);
		swiftbot.startMove(0, 0);
		forwardMovement();	
	}
	//Red light action
	public static void redAction() throws InterruptedException {
		swiftbot.fillUnderlights(redLight);
		swiftbot.startMove(0, 0);
		Thread.sleep(500);
		forwardMovement();
	}
	//Blue light action
	public static void blueAction() throws InterruptedException {
		swiftbot.startMove(0, 0);
		swiftbot.fillUnderlights(blueLight);
		swiftbot.disableUnderlights();
		Thread.sleep(300);
		swiftbot.fillUnderlights(blueLight);
		swiftbot.disableUnderlights();
		Thread.sleep(300);
		swiftbot.fillUnderlights(blueLight);
		swiftbot.disableUnderlights();
		Thread.sleep(300);
		swiftbot.fillUnderlights(blueLight);
		//Turns 90 degree to left
		swiftbot.move(-27, 30, 1000);
		swiftbot.move(30, 30, 1000);
		Thread.sleep(500);
		//Turns 90 degree to right
		swiftbot.move(-30, -30, 1000);
		swiftbot.move(30, -20, 1000);
		forwardMovement();
		
	}
	
	public static void executionLog() throws IOException {
		findFrequentLight();
		if (decider == 1) {
			System.out.println("");
			System.out.println("**Execution Log**");
			System.out.println("");
			System.out.println("Number of traffic lights encountered: " + no_trafficLights);
			System.out.println("Most frequent traffic light: " + frequentLight);
			System.out.println("Number of times the most frequent light was detected: " + no_frequentLight);
			System.out.println("Total duration of execution: " + time + "s");
			createFile();
		} else if (decider == 2) {
			createFile();
		}
	}
	
	//Retrieving the most frequent light encountered by the traffic light.
	public static void findFrequentLight() {
		int greenColor = 0, redColor = 0, blueColor = 0;
		//Calculating how many times each colour has been repeated.
		for (String e: colorList) {
			if (e == "Green") {
				greenColor++;
			} else if (e == "Red") {
				redColor++;
			}else if (e == "Blue") {
				blueColor++;
			}	
		}
		//Determining the colour with greatest amount.
		if ( greenColor> redColor) {
			if (greenColor > blueColor) {
				frequentLight = "Green";
			} else {
				frequentLight = "Blue";
			}
		}else {
			if (redColor > blueColor) {
				frequentLight = "Red";
			} else {
				frequentLight = "Blue";
			}
		}
		//Getting the number of most frequent traffic lights using for each.
		for (String i: colorList) {
			if (i == frequentLight) {
				no_frequentLight++;
			}
		}
		//Calculating the total duration the program.
		time = (end - begin)/1000;
	}
 	//Writing log details in a file.
	public static void createFile() throws IOException {
		 try {
	            FileWriter file = new FileWriter("log.txt");
	            file.write(String.valueOf("Number of traffic lights: " + no_trafficLights));
	            file.write("\t");
	    		file.write("Frequents lights: " + frequentLight);
	    		file.write("\t");
	    		file.write(String.valueOf("Number of frequent lights: " + no_frequentLight));
	    		file.write("\t");
	    		int ltime = (int)time;
	    		file.write("Time: " + String.valueOf(ltime) + "s");
	    		file.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	}
}
