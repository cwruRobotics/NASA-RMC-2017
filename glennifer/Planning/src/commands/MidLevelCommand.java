package commands;

/**
 * Created by Brian on 11/9/2016.
 */
public class MidLevelCommand extends Command{

    // FIELDS


    private MidLevelCommandEnum command;
    private MidLevelCommandPriority priority;


    // ENUM

    public enum MidLevelCommandEnum {

    	//Locomotion
    	MOVE_STRAIGHT,
    	STRAFE,
    	TURN,
    	CONFIGURE,
    		
    	//Excavation
    	SET_ARM_POSITION,
    	SET_TRANSLATION_POSITION,
    	MOVE_BUCKET_CONVEYOR,
    	
    	//Deposition
    	SET_DUMP_POSITION,
    	MOVE_DUMP_CONVEYOR
    		
    }

    public enum MidLevelCommandPriority {

        URGENT,
        HIGH,
        MIDDLE,
        LOW;

        public int compare(MidLevelCommandPriority otherPriority){
            return otherPriority.ordinal() - this.ordinal();
        }


    }


    // CONSTRUCTORS

    public MidLevelCommand(float speed, float timeout, MidLevelCommandEnum command, MidLevelCommandPriority priority){
        super(speed, timeout);
        this.command = command;
        this.priority = priority;
    }

    @Override
    public String toString(){

        StringBuilder builder = new StringBuilder();

        builder.append(command.name() + ": ");
        builder.append("speed=" + getSpeed() + ", ");
        builder.append("timeout=" + getTimeout() + ", ");
        builder.append("priority=" + getPriority().name() + "\n");

        return builder.toString();
    }


    // GET/SET

    public MidLevelCommandEnum getCommand() {

        return command;
    }

    public void setCommand(MidLevelCommandEnum command) {
        this.command = command;
    }

    public MidLevelCommandPriority getPriority() {

        return priority;
    }

    public void setPriority(MidLevelCommandPriority priority) {
        this.priority= priority;
    }

    //

}
