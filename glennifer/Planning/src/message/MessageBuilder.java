package message;

import commands.Command;
import commands.MidLevelCommand;

import java.util.PriorityQueue;

/**
 * Builder class to construct appropriate Messages.
 *
 * Created by Brian on 11/9/2016.
 */
public class MessageBuilder {


    public Message message(String msg){
        Message message = new Message(msg);
        return message;
    }

    public Message readableMessage(String msg){
        return message(msg + '\n');
    }

    public Message commandMessage(Command cmd){
        String msg = cmd.toString();
        Message message = new Message(msg);
        return message;
    }

    public Message queueMessage(PriorityQueue<MidLevelCommand> queue){

        StringBuilder builder = new StringBuilder();

        for(MidLevelCommand c : queue){
            builder.append(c.toString());
        }

        return readableMessage(builder.toString());
    }

}
