package openstackscript;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.*;
import java.util.Scanner;

/**
 *
 * @author debeltrami
 */
public class OCD {

    /**
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws JSchException
     */
    public static void main(String[] inputs, int param) throws FileNotFoundException, IOException, JSchException, InterruptedException {
        String s = null;
        String[] tokens;
        String[] tokensAux;
        String controlPlane, dataPlane, dataPlaneMask, controlPlaneMask;
        String network="net", subnetwork="subnet";
        Scanner entrada = new Scanner(System.in);
        String[] networkID = new String[50];
        String netID;
        String dataPlane2;
        String imageID;
        String output;
        String otNet;
        String otNetMask;
        String rootHost, rootMV, pathFile;
        char[] a;
        int numNetworks, numSubnets, numHosts;
        int itSubNet=1, itHosts=1, itData = 1, itInputs=0, x=0;
        
        
        
        rootHost = inputs[x];
        x++;
        rootMV = inputs[x];
        x++;
        pathFile = inputs[x];
        x++;
        
        
        File arquivo = new File(pathFile);

        
        if (!arquivo.exists()) {
            System.out.println("Error, file does not exist!");
        }

        FileReader fr = new FileReader(arquivo);

        BufferedReader br = new BufferedReader(fr);

        dataPlane = br.readLine();
        tokens = dataPlane.split("[:]");
        
        dataPlane = tokens[1];
        tokens = dataPlane.split("[/]");
        

        dataPlane = tokens[0];
        dataPlane2 = tokens[0];
        dataPlaneMask = tokens[1];

        tokensAux = dataPlane.split("[.]");
        dataPlane = tokensAux[0] + "." + tokensAux[1] + "." + tokensAux[2] + ".";

        controlPlane = br.readLine();
        tokens = controlPlane.split("[:]");
        
        controlPlane = tokens[1];
        tokens = controlPlane.split("[/]");
     
        
        controlPlane = tokens[0];
        controlPlaneMask = tokens[1];
        
        String sshIP = br.readLine();
        tokens = sshIP.split("[:]");
        sshIP = tokens[1];
        
        String sshLogin = br.readLine();
        tokens = sshLogin.split("[:]");
        sshLogin = tokens[1];
        
        
        otNet = br.readLine();
        tokens = otNet.split("[:]");
        otNet = tokens[1];
        
        tokens = otNet.split("[/]");
        otNet = tokens[0];
        otNetMask = tokens[1];
        
        System.out.println(sshIP + " " + sshLogin);
        String command1 = "echo " + rootHost + " | sudo -S ifconfig eth0 " + dataPlane + itData + " netmask 255.255.255.0 up";
        itData++;
        String[] command = {"/bin/bash", "-c", command1};
        
        Process p;
        p = Runtime.getRuntime().exec(command);
        
        command1 = "echo " + rootHost + " | sudo -S route add -net " + otNet + " netmask 255.255.255.0 eth0";
        String[] command2 = {"/bin/bash", "-c", command1};
        
        p = Runtime.getRuntime().exec(command2);
        
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        Session session = jsch.getSession(sshLogin, sshIP, 22);
        session.setPassword(rootMV);
        session.setConfig(config);
        session.connect();
        
        System.out.println("Connected");

        command1 = "echo " + rootMV + " | sudo -S ifconfig eth1 down";
        execCommand(session, command1);
        command1 = "echo " + rootMV + " | sudo -S ifconfig eth1 " + dataPlane + itData + " netmask 255.255.255.0 up"; 
        itData++;
        execCommand(session, command1);
        numNetworks = Integer.parseInt(inputs[x]);
        x++;
        
        
        String[] IPaddress = new String[50];
        
        for ( int i = 1 ; i <= numNetworks ; i++ ){
            
            numSubnets = Integer.parseInt(inputs[x]);
            x++;
            command1 = "source /opt/stack/devstack-stable-grizzly/openrc demo demo ; neutron net-create net" + i;
            output = execCommand(session, command1);
            System.out.println(output);
            
            netID = new String();

            for( int m = 279 ; m <= 315 ; m++ ){
                netID = netID + output.charAt(m);
            }
            networkID[i] = netID; 
            
            System.out.println(netID);
 
            
                        
            for(int j = 1 ; j <= numSubnets ; j++){
                
                IPaddress[itSubNet] = inputs[x];
                x++;
                System.out.println(IPaddress[itSubNet]);
                command1 = "source /opt/stack/devstack-stable-grizzly/openrc demo demo ; neutron subnet-create net" + i + " " +  IPaddress[itSubNet] + " --name subnet" + itSubNet; 
                System.out.println(command1);
                execCommand(session, command1);

                
                numHosts = Integer.parseInt(inputs[x]);
                x++;
                tokens = IPaddress[j].split("[/]");
                IPaddress[j] = tokens[0];
                
                command1 = "echo " + rootHost + " | sudo -S route add -net " + IPaddress[j] + " netmask 255.255.255.0 eth0";
                String[] command3 = {"/bin/bash", "-c", command1};
                p = Runtime.getRuntime().exec(command3);

                command1 = "echo " + rootMV + " | sudo -S ifconfig br-int up ";
                String[] command4 = {"/bin/bash", "-c", command1};
                p = Runtime.getRuntime().exec(command4);

                
                imageID = new String();
                command1 = "source /opt/stack/devstack-stable-grizzly/openrc demo demo ; nova image-list";
                output = execCommand(session,command1);
                
                for (int m = 281; m <= 316; m++) {
                    imageID = imageID + output.charAt(m);

                }
                
                System.out.println(imageID);

         
                for( int k = 0; k < numHosts ; k++ ){
                    command1 = "source /opt/stack/devstack-stable-grizzly/openrc demo demo ; nova boot --image " + imageID + " --flavor 1 --nic net-id=" + networkID[i] + " " + "host" + itHosts;
                    execCommand(session, command1);
                    
                    itHosts++;
                }


                itSubNet++;

            }
            
        }
        
        session.disconnect();
        session = jsch.getSession(sshLogin, sshIP, 22);
        session.setPassword(rootMV);
        session.setConfig(config);
        session.connect();

   
        command1 = "echo " + rootMV + " | sudo -S ovs-ofctl show br-int";
        output = execCommand(session, command1);
        
        int start = 0;
        String[] nTaps = new String[15]; 
        String tap = new String();
        int itTaps = 0;
        
        while (true) {
            int found = output.indexOf("tap", start);
            if (found != -1) {
                for (int m = found; m < found+14; m++) {
                    tap = tap + output.charAt(m);
                }
                               
                nTaps[itTaps] = tap;
                System.out.println(nTaps[itTaps]);
                itTaps++;
            }
            if (found == -1) {
                break;
            }
            start = found + 2;  
        }
        
        
        command1 = "echo " + rootMV + " | sudo -S route del -net " + dataPlane2 + " netmask 255.255.255.0 eth1";
        output = execCommand(session, command1);
        
        command1 = "echo " + rootMV + " | sudo -S brctl addbr br100";
        output = execCommand(session, command1);
        
        command1 = "echo " + rootMV + " | sudo -S brctl addif br100 eth1";
        output = execCommand(session, command1);
        
        for( int i = 0 ; i < itTaps ; i++ ){
            command1 = "echo " + rootMV + " | sudo -S brctl addif br100 " + nTaps[i];
            output = execCommand(session, command1);
        }
         
        command1 = "echo " + rootMV + " | sudo -S ifconfig br100 " + dataPlane + itData + " netmask 255.255.255.0 up";
        itData++;
        output = execCommand(session, command1);
        
        
        for( int i = 1 ; i < itSubNet ; i++ ){
           
            command1 = "echo " + rootMV + " | sudo -S route add -net " + IPaddress[i] + " netmask 255.255.255.0 br100";
            output = execCommand(session, command1);
        }

        for ( int i = 1 ; i < itSubNet ; i++ ){
            command1 = "echo " + rootMV + " | sudo -S route del -net " + IPaddress[i] + " netmask 255.255.255.0 " + nTaps[i-1];
            output = execCommand(session, command1);
        }
        
        command1 = "echo " + rootMV + " | sudo -S route add -net " + otNet + " netmask 255.255.255.0 br100";
        output = execCommand(session, command1);
        
        
        session.disconnect();
        
        System.exit(0);
    }

    private static String execCommand(Session session,String command) throws JSchException, IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        String output=null;
        InputStream in = channel.getInputStream();
        channel.connect();
        
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                output = new String(tmp,0,i);
            }
            if (channel.isClosed()) {
                System.out.println("exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
        
        channel.disconnect();
        
        return output;
        
    }

}

