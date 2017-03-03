package com.cwrubotix.glennifer.hci;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortException;

public class HardwareControlInterface implements Runnable {
	public static final byte COMMAND_READ_SENSORS = 0x01;
	public static final byte COMMAND_SET_OUTPUTS = 0x02;
	
	// The types of actuations and constraints that can be made
	enum ActuationType {
		AngVel, AngPos,
		LinVel, LinPos,
		Torque, Force,
		Current, PowerE,
		PowerM, PowerH,
		Temp;
	}
	
	// Held true when the interface is running 
	private volatile boolean running = false;
	// Queue of actuations to be checked in
	private LinkedBlockingQueue<Actuation> actuationQueue = new LinkedBlockingQueue<Actuation>();
	// Queue of coordinated actuations to be checked in
	private LinkedBlockingQueue<CoordinatedActuation> coordinatedActuationQueue = new LinkedBlockingQueue<CoordinatedActuation>();
	// List of constraints set on various motors, etc.
	private ArrayList<ActuationConstraint> constraints = new ArrayList<ActuationConstraint>();
	// Hashmap of actuators to their ID's
	private HashMap<Integer, Actuator> actuators = new HashMap<Integer,Actuator>();
	// Hashmap of sensors to their ID's
	private HashMap<Integer, Sensor> sensors = new HashMap<Integer, Sensor>();
	// List of active actuation jobs
	private ArrayList<Actuation> activeActuations = new ArrayList<Actuation>();
	// List of active coordinated actuation jobs
	private ArrayList<CoordinatedActuation> activeCoordinatedActuations = new ArrayList<CoordinatedActuation>();
	private final SerialPort port;
	/**
	 * Queue's an actuation to be checked in
	 * @param actuation The actuation job that is to be checked in
	 */
	public void queueActuation(Actuation actuation) {
		actuationQueue.add(actuation);
	}
	
	/**
	 * Queue's a coordinated actuation to be checked in
	 * @param coordinatedActuation The actuation job that is to be checked in
	 */
	public void queueCoordinatedActuation(CoordinatedActuation coordinatedActuation) {
		coordinatedActuationQueue.add(coordinatedActuation);
	}
	
	/**
	 * Halts the interface
	 * @return 0 if success, 1 if it was not running
	 */
	public int halt() {
		if(running) {
			running = false;
			return 0;
		}
		return 1;
	}
	
	/**
	 * Get the sensor object from its ID
	 * @param ID The sensor ID
	 * @return The sensor object
	 */
	public Sensor getSensorFromID(int ID) {
		return sensors.get(ID);
	}
	
	/**
	 * Adds an actuator to the list of actuators
	 * @param actuator The actuator to be added
	 * @param id The ID of the actuator
	 * @return 0 if success, 1 if that ID is already registered
	 */
	public int addActuator(Actuator actuator, int id) {
		if(actuators.containsKey(id)) {
			return 1;
		} else {
			actuators.put(id, actuator);
			return 0;
		}
	}
	
	/**
	 * Adds a sensor to the list of sensors
	 * @param sensor The sensor to be added
	 * @param id The ID of the sensor
	 * @return 0 if success, 1 if that ID is already registered
	 */
	public int addSenor(Sensor sensor, int id) {
		if(sensors.containsKey(id)) {
			return 1;
		} else {
			sensors.put(id, sensor);
			return 0;
		}
	}
	
	@Override
	public void run() {
		running = true;
		try {
			while(running) {
				long t = System.currentTimeMillis();
				// Read sensors
				readSensors();
				// Update actuator data
				for(int id:actuators.keySet()) {
					actuators.get(id).update();
				}
				// Process queue of actuations and coordinated actuations
				for(Actuation a:actuationQueue) {
					if(!addActuation(a)) {
						// Send message that actuation was unsuccessful
						System.out.println("Could not add actuation to actuator ID: " + a.actuatorID);
					}
				}
				// Calculate errors in actuation targets with actuator data
				
				// PID
				
				// Calculate errors in coordinated actuation targets with actuator data
				
				// PID
				calcOutputs();
				// Set outputs
				setOutputs();
				System.out.println(Long.toString(System.currentTimeMillis()-t));
			}
		} catch(SerialPortException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds an Actuation 
	 * @param act
	 * @return
	 */
	private boolean addActuation(Actuation act) {
		if(!act.type.equals(ActuationType.AngVel) && !act.type.equals(ActuationType.LinVel)) {
			// Only support velocities at this moment
			System.out.println("We only support velocity targets at the moment");
			return false;
		}
		// For all active actuations
		for(Actuation a: activeActuations) {
			// If target is already set and override, remove it, otherwise return false
			if(a.actuatorID == act.actuatorID) {
				if(act.override) {
					activeActuations.remove(a);
				} else {
					return false;
				}
			}
		}
		for(CoordinatedActuation ca: activeCoordinatedActuations) {
			// For all active actuations
			if(ca.actuatorID == act.actuatorID) {
				// If target is already set and override, remove it, otherwise return false
				if(act.override) {
					activeCoordinatedActuations.remove(ca);
				} else {
					return false;
				}
			}
		}
		// If no conflict or override, add it
		activeActuations.add(act);
		actuationQueue.remove(act);
		System.out.println("Successfully added actuation on actuator ID: " + act.actuatorID);
		return true;
	}
	
	private void calcOutputs() {
		for(int i = 0; i < activeActuations.size(); i++) {
			activeActuations.get(i).currentOutput = (int) (activeActuations.get(i).targetValue*actuators.get(activeActuations.get(i).actuatorID).config.maxOutput);
		}
	}
	
	private boolean setOutputs() throws SerialPortException {
		if(activeActuations.isEmpty()) {
			return true;
		}
		// Allocate byte array for the data in the request
		byte[] data = new byte[activeActuations.size()*4];
		// Generate data array for request
		// Each actuator ID is 2 bytes, each output is 2 bytes
		for(int i = 0; i < activeActuations.size(); i++) {
			data[3*i] = (byte)(activeActuations.get(i).actuatorID >> 8);
			data[3*i+1] = (byte)(activeActuations.get(i).actuatorID);
			data[3*i+2] = (byte)(activeActuations.get(i).currentOutput >> 8);
			data[3*i+3] = (byte)(activeActuations.get(i).currentOutput);
			System.out.println("Setting output: " + activeActuations.get(i).currentOutput + " actuator ID: " + activeActuations.get(i).actuatorID);
		}
		sendMessage(new SerialPacket(COMMAND_SET_OUTPUTS,data));
		// Get the response
		SerialPacket response = readMessage();
		if(response.command != COMMAND_SET_OUTPUTS && response.data.length <= 0) {
			System.out.println("Failed to set outputs");
			return false;
		}
		System.out.println("Successfully set outputs");
		return true;
	}
	
	private boolean readSensors() throws SerialPortException {
		if(sensors.isEmpty()) {
			return true;
		}
		// Get list of sensor IDs
		Integer[] ids = sensors.keySet().toArray(new Integer[sensors.keySet().size()]);
		// Allocate byte array for the data in the request
		byte[] data = new byte[ids.length*2];
		// Generate data array for request
		// Each sensor ID is 2 bytes
		for(int i = 0; i < ids.length; i++) {
			data[2*i] = (byte)(ids[i].intValue()>>8);
			data[2*i+1] = (byte)ids[i].intValue();
		}
		// Send message, prepares it as per the interface
		sendMessage(new SerialPacket(COMMAND_READ_SENSORS,data));
		// Get the response
		SerialPacket response = readMessage();
		if(response.command != COMMAND_READ_SENSORS) {
			System.out.println("Failed to read sensors");
			return false;
		}
		// Parse the response
		for(int i = 0; i < response.data.length/4; i++) {
			// Parse the sensor IDs
			int sens = ((int)response.data[i*4]) << 8 | response.data[i*4+1];
			// Parse the sensor values
			int dat = ((int)response.data[i*4+2]) << 8 | response.data[i*4+3];
			// If the sensor is not in the hashmap, ignore it
			if(!sensors.containsKey(sens)) {
				System.out.println("Sensor not loaded (ID = " + sens + ")");
				continue;
			}
			// Get the sensor
			Sensor s = sensors.get(sens);
			// Update it with the data
			s.updateRaw(dat);
		}
		return true;
	}
	
	private SerialPacket readMessage() throws SerialPortException {
		byte[] r_head = port.readBytes(2);
		int len = r_head[1];
		byte[] r_body = port.readBytes(len);
		SerialPacket response = new SerialPacket(r_head[0],r_body);
		return response;
	}
	
	private void sendMessage(SerialPacket p) throws SerialPortException {
		port.writeBytes(p.asPacket());
	}
	
	public HardwareControlInterface(SerialPort sp) {
		port = sp;
	}
	
}