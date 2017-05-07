
datestring=$(date +%Y_%m_%d_%H_%M_%S)
cd /home/ubuntu/
mkdir -p "logs/$datestring"

cd /home/ubuntu/workspace/NASA-RMC-2017/

# start the things
cd glennifer/HCI
nohup mvn exec:java -Dexec.mainClass="com.cwrubotix.glennifer.hci.ModuleMain" &> "/home/ubuntu/logs/$datestring/ModuleMainOutput.log" &
cd ..

cd robot_state
nohup mvn exec:java -Dexec.mainClass="com.cwrubotix.glennifer.robot_state.StateModule" &> "/home/ubuntu/logs/$datestring/StateModuleOutput.log" &
cd ..

cd motor_dispatch/src/python
nohup python3 locomotion.py &> "/home/ubuntu/logs/$datestring/LocomotionPyOutput.log" &
cd ../../../..

cd client-cameras
nohup python client-cam-send.py &> "/home/ubuntu/logs/$datestring/CameraSendPyOutput.log" &
cd ..

cd ..
