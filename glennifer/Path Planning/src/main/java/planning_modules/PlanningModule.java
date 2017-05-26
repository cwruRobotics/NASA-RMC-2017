package planning_modules;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import commands.HighLevelCommand;
import commands.MidLevelCommand;

public class PlanningModule {

	private final static String RECEIVE_QUEUE = "HighCommands";
	private final static String SEND_QUEUE = "MidCommands";
	private final static String COMMAND_EXCHANGE = "amq.topic";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		factory.setUsername("guest");
		factory.setPassword("guest");
		Connection connection = factory.newConnection();

		subscribe(connection);
		publish(connection, "Hello");

		boolean run = true;
		Scanner scan = new Scanner(System.in);
		String input;
		while (run) {
			input = scan.nextLine();
			if (input.equals("quit"))
				run = false;
			else
				publish(connection, input);
		}
		scan.close();
		Thread.sleep(10000000);
	}

	/**
	 * This method is to be used if the commands are to be sent to a specific
	 * module
	 * 
	 * @param factory
	 * @param message
	 * @throws java.io.IOException
	 * @throws TimeoutException
	 */
	private static void send(Connection connection, String message) throws java.io.IOException, TimeoutException {
		Channel channel = connection.createChannel();

		channel.queueDeclare(SEND_QUEUE, false, false, false, null);
		channel.basicPublish("", SEND_QUEUE, null, message.getBytes("UTF-8"));
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		connection.close();

	}

	/**
	 * This method is to be used if the Commands will be published to an
	 * exchange
	 * 
	 * @param factory
	 * @param message
	 * @throws java.io.IOException
	 * @throws TimeoutException
	 */
	private static void publish(Connection connection, String message) throws java.io.IOException, TimeoutException {
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(COMMAND_EXCHANGE, "topic", true);

		channel.basicPublish(COMMAND_EXCHANGE, "HighCommand", null, message.getBytes());
		System.out.println(" [x] Sent '" + message + "'");

		channel.close();
		// connection.close();
	}

	/**
	 * This message is to subscribe to the robot state modules
	 * 
	 * @param factory
	 * @throws Exception
	 */
	private static void subscribe(Connection connection) throws Exception {
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(COMMAND_EXCHANGE, "topic", true);
		String queueName = channel.queueDeclare().getQueue();
		channel.queueBind(queueName, COMMAND_EXCHANGE, "HighCommand");

		// System.out.println(" [*] Waiting for messages. To exit press
		// CTRL+C");

		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String message = new String(body, "UTF-8");
				System.out.println(" [x] Received '" + message + "'");

				updateState(message);
			}
		};
		channel.basicConsume(queueName, true, consumer);
	}

	/**
	 * Interprets the information from the state modules
	 * 
	 * @param info
	 */
	private static void updateState(String info) {
		// TODO put stuff about how to update the state info here
	}

	/**
	 * Parses the information from received HighLevelCommands
	 * 
	 * @param command
	 */
	private static void receiveCommand(String command) {
		Queue<MidLevelCommand> command_queue = new LinkedList<MidLevelCommand>();

		String[] contents = command.split("\\.");
		HighLevelCommand high_command = translate(command);
		String type = contents[0];

		if (type.equals("Deposition")) {
			// TODO put stuff here for deposition commands
			// command_queue = Deposition.receiveCommand(stuff);
		} else if (type.equals("Excavation")) {
			// TODO put stuff here excavation commands
			// command_queue = Excavation.receiveCommand(stuff);
		} else if (type.equals("Locomotion")) {
			// TODO put stuff here locomotion commands
			// command_queue = Locomotion.receiveCommand(stuff);
		}
	}

	private static HighLevelCommand translate(String command) {
		// This is just a placeholder until I know how the information will be
		// represented
		HighLevelCommand temp = new HighLevelCommand(1, 5f);
		return temp;
	}
	
	message killMe{
		
	}

}
