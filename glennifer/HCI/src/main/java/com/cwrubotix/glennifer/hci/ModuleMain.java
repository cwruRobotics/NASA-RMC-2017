package com.cwrubotix.glennifer.hci;

import com.cwrubotix.glennifer.Messages;

import com.rabbitmq.client.*;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ModuleMain {
	public static final int baud = 9600;
	
	/* Listen for Topics */
	//Motor Controls
	public static final String motorTopic = "motorcontrol.#";
	
	private static HardwareControlInterface hci;

	public static void runWithConnectionExceptions() throws IOException, TimeoutException {
		// Read connection config
		InputStream input = new FileInputStream("config/connection.yml");
		Yaml yaml = new Yaml();
		Object connectionConfigObj = yaml.load(input);
		Map<String, String> connectionConfig = (Map<String, String>)connectionConfigObj;
		String serverAddress = connectionConfig.get("server-addr");
		if (serverAddress == null) {
			throw new RuntimeException("Config file missing server-addr");
		}
		String serverUsername = connectionConfig.get("server-user");
		if (serverUsername == null) {
			throw new RuntimeException("Config file missing server-user");
		}
		String serverPassword = connectionConfig.get("server-pass");
		if (serverPassword == null) {
			throw new RuntimeException("Config file missing server-pass");
		}
		String exchangeName = connectionConfig.get("exchange-name");
		if (exchangeName == null) {
			throw new RuntimeException("Config file missing exchange-name");
		}

		//Connect and Configure AMPQ
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(serverAddress); //replace local host with host name
		factory.setUsername(serverUsername);
		factory.setPassword(serverPassword);
		Connection connection = factory.newConnection(); // throws
		Channel channel = connection.createChannel(); // throws
		String queueName = channel.queueDeclare().getQueue();

		// Initialize port as null
		String port = null;
		// For each attached serial port
		for(String s:SerialPortList.getPortNames()) {
			SerialPort sp = new SerialPort(s);
			try {
				// Open the port
				sp.openPort();
				sp.setParams(baud, 8, 1, 0);
				sp.setDTR(false);
				// Create test packet
				byte[] bt = {0x5A,0x01,0x00};
				// Write test byte 0x5A
				sp.writeBytes(bt);
				Thread.sleep(2000);
				// Read response bytes
				byte[] b = sp.readBytes(1,1000);
				// If response is 0xA5, it is the arduino
				if(b[0] == (byte)0xA5) {
					// Capture the string of correct port
					port = s;
					// Close the port
					sp.closePort();
					break;
				}
				sp.closePort();
			} catch(SerialPortException e) {
				e.printStackTrace();
				continue;
			} catch(SerialPortTimeoutException e) {
				try {
					sp.closePort();
				} catch(Exception e1) {
					e1.printStackTrace();
				}
				continue;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// If port is still null, couldn't find it
		if(port == null) {
			System.out.println("Couldn't find attached arduino, please try again");
			return;
		} else {
			System.out.println("Found arduino at " + port);
		}
		hci = new HardwareControlInterface(port);
                
		// Initialize sensors
                
                /* Locomotion */
                SensorConfig configFLRPM = new SensorConfig();
                configFLRPM.ID = 0;
                configFLRPM.name = "Front Left Wheel Encoder";
                configFLRPM.description = "DESC";
                configFLRPM.limitSwitch = false;
                configFLRPM.scale = 1;
                
                SensorConfig configFRRPM = configFLRPM.copy();
                configFRRPM.ID = 1;
                configFRRPM.name = "Front Right Wheel Encoder";
                
                SensorConfig configBLRPM = configFLRPM.copy();
                configBLRPM.ID = 2;
                configBLRPM.name = "Back Left Wheel Encoder";
                
                SensorConfig configBRRPM = configFLRPM.copy();
                configBRRPM.ID = 3;
                configBRRPM.name = "Back Right Wheel Encoder";
                
                SensorConfig configFLPOS = new SensorConfig();
                configFLPOS.ID = 4;
                configFLPOS.name = "Front Left Wheel Pod Position";
                configFLPOS.description = "DESC";
                configFLPOS.limitSwitch = false;
                configFLPOS.scale = 1;
                
                SensorConfig configFLPOSEX = configFLPOS.copy();
                configFLPOSEX.ID = 5;
                configFLPOSEX.name = "Front Right Wheel Pod Limit 90";
                configFLPOSEX.limitSwitch = true;
                
                SensorConfig configFLPOSRE = configFLPOS.copy();
                configFLPOSRE.ID = 6;
                configFLPOSRE.name = "Front Right Wheel Pod Limit 0";
                configFLPOSRE.limitSwitch = true;
                
                SensorConfig configFRPOS = configFLPOS.copy();
                configFRPOS.ID = 7;
                configFRPOS.name = "Front Right Wheel Pod Position";
                
                SensorConfig configFRPOSEX = configFLPOS.copy();
                configFRPOSEX.ID = 8;
                configFRPOSEX.name = "Front Right Wheel Pod Limit 90";
                configFRPOSEX.limitSwitch = true;
                
                SensorConfig configFRPOSRE = configFLPOS.copy();
                configFRPOSRE.ID = 9;
                configFRPOSRE.name = "Front Left Wheel Pod Limit 0";
                configFRPOSRE.limitSwitch = true;
                
                SensorConfig configBLPOS = configFLPOS.copy();
                configBLPOS.ID = 10;
                configBLPOS.name = "Back Left Wheel Pod Position";
                
                SensorConfig configBLPOSEX = configFLPOS.copy();
                configBLPOSEX.ID = 11;
                configBLPOSEX.name = "Back Left Wheel Pod Limit 90";
                configBLPOSEX.limitSwitch = true;
                
                SensorConfig configBLPOSRE = configFLPOS.copy();
                configBLPOSRE.ID = 12;
                configBLPOSRE.name = "Back Left Wheel Pod Limit 0";
                configBLPOSRE.limitSwitch = true;
                        
                SensorConfig configBRPOS = configFLPOS.copy();
                configBRPOS.ID = 13;
                configBRPOS.name = "Back Right Wheel Pod Position";
                
                SensorConfig configBRPOSEX = configFLPOS.copy();
                configBRPOSEX.ID = 14;
                configBRPOSEX.name = "Back Right Wheel Pod Limit 90";
                configBRPOSEX.limitSwitch = true;
                
                SensorConfig configBRPOSRE = configFLPOS.copy();
                configBRPOSRE.ID = 15;
                configBRPOSRE.name = "Back Right Wheel Pod Limit 0";
                configBRPOSRE.limitSwitch = true;
                
                //ADD LIMIT SENSORS
                
                /* Excavation */
                SensorConfig configARMPOSA = new SensorConfig(); 
                configARMPOSA.ID = 16;
                configARMPOSA.name = "Arm Angle Pot Position A";
                configARMPOSA.description = "DESC";
                configARMPOSA.limitSwitch = false;
                configARMPOSA.scale = 1;
                
                SensorConfig configARMPOSAL = configARMPOSA.copy();
                configARMPOSAL.ID = 17;
                configARMPOSAL.name = "Arm A Limit Low";
                configARMPOSAL.limitSwitch = true;
                
                SensorConfig configARMPOSAH = configARMPOSA.copy();
                configARMPOSAH.ID = 18;
                configARMPOSAH.name = "Arm A Limit High";
                configARMPOSAH.limitSwitch = true;
                
                SensorConfig configARMPOSB = configARMPOSA.copy();
                configARMPOSB.ID = 19;
                configARMPOSB.name = "Arm Angle Pot Position B";
                
                SensorConfig configARMPOSBL = configARMPOSA.copy();
                configARMPOSBL.ID = 20;
                configARMPOSBL.name = "Arm B Limit Low";
                configARMPOSBL.limitSwitch = true;
                
                SensorConfig configARMPOSBH = configARMPOSA.copy();
                configARMPOSBH.ID = 21;
                configARMPOSBH.name = "Arm B Limit High";
                configARMPOSBH.limitSwitch = true;

                SensorConfig configTPOSA = configARMPOSA.copy();
                configTPOSA.ID = 22;
                configTPOSA.name = "Translation Pot Position A";
                
                SensorConfig configTPOSAL = configARMPOSA.copy();
                configTPOSAL.ID = 23;
                configTPOSAL.name = "Translation Pot A Limit Low";
                configTPOSAL.limitSwitch = true;
                
                SensorConfig configTPOSAH = configARMPOSA.copy();
                configTPOSAH.ID = 23;
                configTPOSAH.name = "Translation Pot A Limit High";
                configTPOSAH.limitSwitch = true;
                
                SensorConfig configTPOSB = configARMPOSA.copy();
                configTPOSB.ID = 24;
                configTPOSB.name = "Translation Pot Position B";
                
                SensorConfig configTPOSBL = configARMPOSA.copy();
                configTPOSBL.ID = 25;
                configTPOSBL.name = "Translation Pot B Limit Low";
                configTPOSBL.limitSwitch = true;
                
                SensorConfig configTPOSBH = configARMPOSA.copy();
                configTPOSBH.ID = 26;
                configTPOSBH.name = "Translation Pot B Limit High";
                configTPOSBH.limitSwitch = true;
                
                SensorConfig configBELT = new SensorConfig();
                configBELT.ID = 27;
                configBELT.name = "Belt Encoder Speed";
                configBELT.description = "DESC";
                configBELT.limitSwitch = false;
                configBELT.scale = 1;
                
                /* Deposition */
                
                SensorConfig configCELLA = new SensorConfig();
                configCELLA.ID = 28;
                configCELLA.name = "Load Cell A";
                configCELLA.description = "DESC";
                configCELLA.limitSwitch = false;
                configCELLA.scale = 1;
                
                SensorConfig configCELLB = configCELLA.copy();
                configCELLB.ID = 29;
                configCELLB.name = "Load Celll B";
                
                SensorConfig configCELLC = configCELLA.copy();
                configCELLC.ID = 30;
                configCELLC.name = "Load Cell C";
                
                SensorConfig configCELLD = configCELLA.copy();
                configCELLD.ID = 31;                
                configCELLD.name = "Load Cell D";

                
                SensorConfig configDEPOSA = new SensorConfig();
                configDEPOSA.ID = 32;
                configDEPOSA.name = "Deposition Actuator Pot Position A";
                configDEPOSA.description = "DESC";
                configDEPOSA.limitSwitch = false;
                configDEPOSA.scale = 1;
                
                SensorConfig configDEPOSAL = configDEPOSA.copy();
                configDEPOSAL.ID = 33;
                configDEPOSAL.name = "Deposition Actuator Pot A Limit Low";
                configDEPOSAL.limitSwitch = true;
                
                SensorConfig configDEPOSAH = configDEPOSA.copy();
                configDEPOSAH.ID = 34;
                configDEPOSAH.name = "Deposition Actuator Pot A Limit High";
                configDEPOSAH.limitSwitch = true;
                
                SensorConfig configDEPOSB = configDEPOSA.copy();
                configDEPOSB.ID = 35;
                configDEPOSB.name = "Deposition Actuator Pot Position B";
                
                SensorConfig configDEPOSBL = configDEPOSA.copy();
                configDEPOSBL.ID = 36;
                configDEPOSBL.name = "Deposition Actuator Pot B Limit Low";
                configDEPOSBL.limitSwitch = true;
                
                SensorConfig configDEPOSBH = configDEPOSA.copy();
                configDEPOSBH.ID = 37;
                configDEPOSBH.name = "Deposition Actuator Pot B Limit High";
                configDEPOSBH.limitSwitch = true;
               
		// Add sensors
                hci.addSensor(new Sensor(configFLRPM), configFLRPM.ID);
                hci.addSensor(new Sensor(configFRRPM), configFRRPM.ID);
                hci.addSensor(new Sensor(configBLRPM), configBLRPM.ID);
                hci.addSensor(new Sensor(configBRRPM), configBRRPM.ID);
                hci.addSensor(new Sensor(configFLPOS), configFLPOS.ID);
                hci.addSensor(new Sensor(configFLPOSEX), configFLPOSEX.ID);
                hci.addSensor(new Sensor(configFLPOSRE), configFLPOSRE.ID);
                hci.addSensor(new Sensor(configFLPOS), configFRPOS.ID);
                hci.addSensor(new Sensor(configFLPOSEX), configFRPOSEX.ID);
                hci.addSensor(new Sensor(configFLPOSRE), configFRPOSRE.ID);
                hci.addSensor(new Sensor(configBLPOS), configBLPOS.ID);
                hci.addSensor(new Sensor(configBLPOSEX), configBLPOSEX.ID);
                hci.addSensor(new Sensor(configBLPOSRE), configBLPOSRE.ID);
                hci.addSensor(new Sensor(configBRPOS), configBRPOS.ID);
                hci.addSensor(new Sensor(configBRPOSEX), configBRPOSEX.ID);
                hci.addSensor(new Sensor(configBRPOSRE), configBRPOSRE.ID);
                hci.addSensor(new Sensor(configARMPOSA), configARMPOSA.ID);
                hci.addSensor(new Sensor(configARMPOSAL), configARMPOSAL.ID);
                hci.addSensor(new Sensor(configARMPOSAH), configARMPOSAH.ID);
                hci.addSensor(new Sensor(configARMPOSB), configARMPOSB.ID);
                hci.addSensor(new Sensor(configARMPOSBL), configARMPOSBL.ID);
                hci.addSensor(new Sensor(configARMPOSBH), configARMPOSBH.ID);
                hci.addSensor(new Sensor(configTPOSA), configTPOSA.ID);
                hci.addSensor(new Sensor(configTPOSAL), configTPOSAL.ID);
                hci.addSensor(new Sensor(configTPOSAH), configTPOSAH.ID);
                hci.addSensor(new Sensor(configTPOSB), configTPOSB.ID);
                hci.addSensor(new Sensor(configTPOSBL), configTPOSBL.ID);
                hci.addSensor(new Sensor(configTPOSBH), configTPOSBH.ID);
                hci.addSensor(new Sensor(configBELT), configBELT.ID);
                hci.addSensor(new Sensor(configCELLA), configCELLA.ID);
                hci.addSensor(new Sensor(configCELLB), configCELLB.ID);
                hci.addSensor(new Sensor(configCELLC), configCELLC.ID);
                hci.addSensor(new Sensor(configCELLD), configCELLD.ID);
                hci.addSensor(new Sensor(configDEPOSA), configDEPOSA.ID);
                hci.addSensor(new Sensor(configDEPOSAL), configDEPOSAL.ID);
                hci.addSensor(new Sensor(configDEPOSAH), configDEPOSAH.ID);
                hci.addSensor(new Sensor(configDEPOSB), configDEPOSB.ID);
                hci.addSensor(new Sensor(configDEPOSBL), configDEPOSBL.ID);
                hci.addSensor(new Sensor(configDEPOSBH), configDEPOSBH.ID);

                // Initialize actuators
                
		ActuatorConfig configLBM = new ActuatorConfig();
		configLBM.ID = 0;
		configLBM.name = "Left Rear Drive Motor";
		configLBM.description = "Vex BAG Motor, 270:1";
		configLBM.anglin = true;
		configLBM.nomVoltage = 12;
		configLBM.noLoadCurrent = 1.8;
		configLBM.noLoadVel = (13180/270)*2*Math.PI/60;
		configLBM.stallCurrent = 53;
		configLBM.tfStall = 0.43*270;
		configLBM.tfCurrentRatio = (configLBM.stallCurrent - configLBM.noLoadCurrent)/configLBM.tfStall;

		ActuatorConfig configRBM = configLBM.copy();
		configRBM.ID = 1;
		configRBM.name = "Right Rear Drive Motor";

		ActuatorConfig configLFM = configLBM.copy();
		configLFM.ID = 2;
		configLFM.name = "Right Front Drive Motor";

		ActuatorConfig configRFM = configLBM.copy();
		configRFM.ID = 3;
		configRFM.name = "Right Front Drive Motor";

		ActuatorConfig configLBA = new ActuatorConfig();
		configLBA.ID = 4;
		configLBA.name = "Left Rear Turning Actuator";
		configLBA.description = "Progressive Automations PA-14P-4-150";
		configLBA.anglin = false;
		configLBA.nomVoltage = 12;
		configLBA.noLoadCurrent = 0;
		configLBA.noLoadVel = 0.015;
		configLBA.stallCurrent = 5;
		configLBA.tfStall = 33.7;
		configLBA.tfCurrentRatio = (configLBA.stallCurrent - configLBA.noLoadCurrent)/configLBA.tfStall;

		ActuatorConfig configRBA = configLBA.copy();
		configRBA.ID = 5;
		configRBA.name = "Right Rear Turning Actuator";

		ActuatorConfig configLFA = configLBA.copy();
		configLFA.ID = 6;
		configLFA.name = "Left Front Turning Actuator";

		ActuatorConfig configRFA = configLBA.copy();
		configRFA.ID = 7;
		configRFA.name = "Right Front Turning Actuator";

		// Add actuators
		hci.addActuator(new Actuator(configLBM, hci), configLBM.ID);
		hci.addActuator(new Actuator(configRBM, hci), configRBM.ID);
		hci.addActuator(new Actuator(configLFM, hci), configLFM.ID);
		hci.addActuator(new Actuator(configRFM, hci), configRFM.ID);
		hci.addActuator(new Actuator(configLBA, hci), configLBA.ID);
		hci.addActuator(new Actuator(configRBA, hci), configRBA.ID);
		hci.addActuator(new Actuator(configLFA, hci), configLFA.ID);
		hci.addActuator(new Actuator(configRFA, hci), configRFA.ID);

		// Constrain actuators

		// Start HCI

		Thread hciThread = new Thread(hci);
		hciThread.start();
		/*
		try {
			Thread.sleep(100);
			Actuation a = new Actuation();
			a.override = true;
			a.hold = true;
			a.targetValue = 1;
			a.type = HardwareControlInterface.ActuationType.AngVel;
			a.actuatorID = 0;
			hci.queueActuation(a);
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/

		//Start AMPQ Thread
		//Listen for messages
		//if

		channel.queueBind(queueName, exchangeName, motorTopic);

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
									   AMQP.BasicProperties properties, byte[] body) throws IOException {
				String routingKey = envelope.getRoutingKey();
				String[] keys = routingKey.split("\\.");
				if(keys.length < 4) {
					System.out.println("Failed to interpret routing key");
					return;
				}
				if (keys[3].equals("wheel_rpm")) {
				    int id = -1;
				    if (keys[2].equals("front_left")) {
				        id = 0;
                    } else if (keys[2].equals("front_right")) {
				        id = 1;
                    } else if (keys[2].equals("back_left")) {
				        id = 2;
                    } else if (keys[2].equals("back_right")) {
				        id = 3;
                    } else {
				        // TODO: handle this
                    }
                    Messages.SpeedContolCommand scc = Messages.SpeedContolCommand.parseFrom(body);
					Actuation a = new Actuation();
					a.override = true;
					a.hold = true;
					if (id % 2 == 0) {
                        a.targetValue = scc.getRpm() * -32767.0F;
                    } else {
                        a.targetValue = scc.getRpm() * 32767.0F;
                    }
					a.type = HardwareControlInterface.ActuationType.AngVel;
					a.actuatorID = id;
					System.out.println("queueing actuaton for id = " + id);
					hci.queueActuation(a);

				} else if (keys[3].equals("wheel_pod_pos")) {
                    int id = -1;
                    if (keys[2].equals("front_left")) {
                        id = 4;
                    } else if (keys[2].equals("front_right")) {
                        id = 5;
                    } else if (keys[2].equals("back_left")) {
                        id = 6;
                    } else if (keys[2].equals("back_right")) {
                        id = 7;
                    } else {
                        // TODO: handle this
                    }
					Messages.PositionContolCommand pcc = Messages.PositionContolCommand.parseFrom(body);
					Actuation a = new Actuation();
					a.override = true;
					a.hold = true;
					//a.targetValue = pcc.getPosition()*5 + 250;
					a.targetValue = 1023-(.04624+0.79547*(1.03586+1.50175*Math.sin(Math.PI*(pcc.getPosition()+316.63691)/180.2324)))*(1024/3.3);
					System.out.println(a.targetValue);
					a.type = HardwareControlInterface.ActuationType.AngVel;
					a.actuatorID = id;
					hci.queueActuation(a);
				}
			}
		};
		channel.basicConsume(queueName, true, consumer);

		// Main loop to get sensor data
		try {
			while (true) {
				Sensor s = hci.getSensorFromID(4);
				if (s.data.isEmpty()) {
					System.out.println("Sensor #4 has no data");
				} else {
					System.out.println("Sensor #4 = " + s.data.get(s.data.size()-1).data);
				}
                s = hci.getSensorFromID(5);
                if (s.data.isEmpty()) {
                    System.out.println("Sensor #5 has no data");
                } else {
                    System.out.println("Sensor #5 = " + s.data.get(s.data.size()-1).data);
                }
                s = hci.getSensorFromID(6);
                if (s.data.isEmpty()) {
                    System.out.println("Sensor #6 has no data");
                } else {
                    System.out.println("Sensor #6 = " + s.data.get(s.data.size()-1).data);
                }
                s = hci.getSensorFromID(7);
                if (s.data.isEmpty()) {
                    System.out.println("Sensor #7 has no data");
                } else {
                    System.out.println("Sensor #7 = " + s.data.get(s.data.size()-1).data);
                }
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) { }
	}

	private static int sign(double x) {
		if (x > 0) {
			return 1;
		} else if (x < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	public static void main(String[] args) {
		try {
			runWithConnectionExceptions();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}



	}

}
