package com.cwrubotix.glennifer.autodrill;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import com.rabbitmq.client.AMQP;

import com.cwrubotix.glennifer.Messages;
import com.cwrubotix.glennifer.Messages.LocomotionControlCommandStraight;
import com.cwrubotix.glennifer.Messages.SpeedContolCommand;
import com.cwrubotix.glennifer.Messages.Fault;
import com.cwrubotix.glennifer.Messages.UnixTime;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Reads and sends messages needed to auto drill.
 *
 */
public class AutoDrillModule {

	private float currentUpperLimit = 7.0F;
	private float currentLowerLimit = 3.0F;
	
	private class DrillDeepConsumer extends DefaultConsumer{
		public DrillDeepConsumer(Channel channel){
			super(channel);
		}
		
		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
			// Transition to dig deep
			currentJob = DrillJob.DEEP;
			Messages.ExcavationControlCommandDigDeep cmd = Messages.ExcavationControlCommandDigDeep.parseFrom(body);
			targetDepth = cmd.getDepth();
			digSpeed = cmd.getDigSpeed();
			modeStartTime = Instant.now();
			updateMotors();
		}
	}
	
	private class DrillSurfaceConsumer extends DefaultConsumer{
		public DrillSurfaceConsumer(Channel channel){
			super(channel);
		}
		
		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
			currentJob = DrillJob.SURFACE;
			Messages.ExcavationControlCommandDigSurface cmd = Messages.ExcavationControlCommandDigSurface.parseFrom(body);
			targetDepth = cmd.getDepth();
			digSpeed = cmd.getDigSpeed();
			targetDist = cmd.getDist();
			driveSpeed = cmd.getDriveSpeed();
			modeStartTime = Instant.now();
			updateMotors();
		}
	}
	
	private class DrillEndConsumer extends DefaultConsumer{
		public DrillEndConsumer(Channel channel){
			super(channel);
		}
		
		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
			currentJob = DrillJob.NONE;
			updateMotors();
		}
	}

	private enum DrillJob {DEEP, SURFACE, NONE}

	private String exchangeName;
	private Connection connection;
	private Channel channel;

	private DrillJob currentJob = DrillJob.SURFACE;
	private float targetDepth = 50.0F;
	private float targetDist = 0.0F;
	private float digSpeed = 2.0F;
	private float driveSpeed = 0.5F;
	private Instant modeStartTime;
	private float modeStartDepth = 10.0F;
	private boolean isStalled = false;

	private float bc_trans = 0.0F;
	private float bc_angle = 0.0F;

	private void updateMotors() {
		
		try {
			switch (currentJob) {
				case NONE:
					excavationConveyorRPM(0);
					excavationTranslationControl(0);
					locomotionSpeedControl(0.0F);
					break;
				case DEEP:
					if (isStalled) {
						excavationConveyorRPM(100);
						excavationTranslationControl(0);
					} else {
						excavationConveyorRPM(100);
						excavationTranslationControl(getCurrentDepthTarget());
					}
					break;
				case SURFACE:
					if (isStalled) {
						excavationConveyorRPM(100);
						excavationTranslationControl(0);
					} else {
						excavationConveyorRPM(100);
						excavationTranslationControl(getCurrentDepthTarget());
						if (bc_trans < (targetDepth - 10)) {
							locomotionSpeedControl(0.0F);
						} else {
							locomotionSpeedControl(driveSpeed);
						}
					}
					break;
			}
		} catch (IOException e) {
			System.out.println("AutoDrill failed to publish message with exception:");
			e.printStackTrace();
		}
	}

	private float getCurrentDepthTarget() {
		float calculatedDepth = modeStartDepth + (Duration.between(modeStartTime, Instant.now()).toMillis() / 1000.0F) * digSpeed;
		return calculatedDepth < targetDepth ? calculatedDepth : targetDepth;
	}
	
	private void excavationTranslationControl(float targetValue) throws IOException{
		Messages.PositionContolCommand pcc = Messages.PositionContolCommand.newBuilder()
				.setPosition(targetValue)
				.setTimeout(123)
				.build();
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.excavation.conveyor_translation_displacement", null, pcc.toByteArray());
	}
	
	private void excavationAngleControl(float targetValue) throws IOException{
		Messages.PositionContolCommand pcc = Messages.PositionContolCommand.newBuilder()
				.setPosition(targetValue)
				.setTimeout(123)
				.build();
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.excavation.arm_pos", null, pcc.toByteArray());
	}
	
	private void excavationConveyorRPM(float targetValue) throws IOException{
		Messages.SpeedContolCommand msg = SpeedContolCommand.newBuilder()
				.setRpm(targetValue)
				.setTimeout(123)
				.build();
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.excavation.bucket_conveyor_rpm", null, msg.toByteArray());
	}
	
	private void locomotionSpeedControl(float targetValue) throws IOException{
		Messages.SpeedContolCommand msg = SpeedContolCommand.newBuilder()
				.setRpm(targetValue)
				.setTimeout(123)
				.build();
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.locomotion.front_left.wheel.RPM", null, msg.toByteArray());
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.locomotion.front_right.wheel.RPM", null, msg.toByteArray());
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.locomotion.back_right.wheel.RPM", null, msg.toByteArray());
		AutoDrillModule.this.channel.basicPublish(exchangeName, "motorcontrol.locomotion.back_left.wheel.RPM", null, msg.toByteArray());
	}

	private void locomotionStraight() throws IOException{
		Messages.LocomotionControlCommandStraight msg = LocomotionControlCommandStraight.newBuilder()
				.setTimeout(123)
				.setSpeed(0.5F)
				.build();
		AutoDrillModule.this.channel.basicPublish(exchangeName, "subsyscommand.locomotion.straight", null, msg.toByteArray());
	}
	
	public AutoDrillModule(){
		this("amq.topic");
	}
	
	public AutoDrillModule(String exchangeName){
		this.exchangeName = exchangeName;
	}
	
	private UnixTime instantToUnixTime(Instant time) {
		UnixTime.Builder unixTimeBuilder = UnixTime.newBuilder();
		unixTimeBuilder.setTimeInt(time.getEpochSecond());
		unixTimeBuilder.setTimeFrac(time.getNano() / 1000000000F);
		return unixTimeBuilder.build();
	}
	    
	private void sendFault(int faultCode, Instant time) throws IOException {
		Fault.Builder faultBuilder = Fault.newBuilder();
		faultBuilder.setFaultCode(faultCode);
		faultBuilder.setTimestamp(instantToUnixTime(time));
		Fault message = faultBuilder.build();
		channel.basicPublish(exchangeName, "fault", null, message.toByteArray());
	}
	
	public void runWithExceptions() throws IOException, TimeoutException{
		//Setup connection
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		this.connection = factory.newConnection();
		this.channel = connection.createChannel();
		
		//Listen for DrillDeep command
		String queueName = channel.queueDeclare().getQueue();
		channel.queueBind(queueName, exchangeName, "drill.deep");
		this.channel.basicConsume(queueName, true, new DrillDeepConsumer(channel));
		
		//Listen for DrillSurface command
		queueName = channel.queueDeclare().getQueue();
		channel.queueBind(queueName, exchangeName, "drill.surface");
		this.channel.basicConsume(queueName, true, new DrillSurfaceConsumer(channel));
		
		//Listen for DrillEnd command
		queueName = channel.queueDeclare().getQueue();
		channel.queueBind(queueName, exchangeName, "drill.end");
		this.channel.basicConsume(queueName, true, new DrillEndConsumer(channel));

		Messages.StateSubscribe msg = Messages.StateSubscribe.newBuilder()
				  .setReplyKey("autoDrillModule")
				  .setInterval(0.1F)
				  .setDepositionDetailed(false)
				  .setDepositionSummary(true)
				  .setExcavationDetailed(true)
				  .setExcavationSummary(false)
				  .setLocomotionDetailed(true)
				  .setLocomotionSummary(false)
				  .build();
		this.channel.basicPublish(exchangeName, "state.subscribe", null, msg.toByteArray());
		
		// Enter main loop
		modeStartTime = Instant.now();
		queueName = channel.queueDeclare().getQueue();
		channel.queueBind(queueName, exchangeName, "autoDrillModule");
		this.channel.basicConsume(queueName, true, new DefaultConsumer(channel){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException{
				Messages.State msg = Messages.State.parseFrom(body);
			    bc_trans = msg.getExcDetailed().getDisplacement();
			    float bc_current = msg.getExcDetailed().getConveyorMotorCurrent();
			    bc_angle = msg.getExcDetailed().getArmPos();
			    
			    if(!isStalled && bc_current > currentUpperLimit) {
					// Transition to stalled
					isStalled = true;
				} else if (isStalled && bc_current <= currentLowerLimit) {
					// Transition to unstalled
					isStalled = false;
					modeStartTime = Instant.now();
					modeStartDepth = bc_trans;
				}
				updateMotors();
			}
		});
		
	}
	
	public void start(){
		try{
			runWithExceptions();
		} catch(Exception e){
			try{
				sendFault(999, Instant.now());
			} catch(Exception e1){}
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	public void stop() {
		try {
			channel.close();
			connection.close();
			Messages.StateSubscribe msg = Messages.StateSubscribe.newBuilder()
												  .setReplyKey("autoDrillModule")
												  .setInterval(0.2F)
												  .setDepositionDetailed(false)
												  .setDepositionSummary(true)
												  .setExcavationDetailed(true)
												  .setExcavationSummary(false)
												  .setLocomotionDetailed(true)
												  .setLocomotionSummary(false)
												  .build();
			this.channel.basicPublish(exchangeName, "state.unsubscribe", null, msg.toByteArray());
		} catch (IOException | TimeoutException e) {
			// Do nothing
		}
	}
	
	public static void main(String[] args){
		AutoDrillModule module = new AutoDrillModule();
		module.start();
	}
}