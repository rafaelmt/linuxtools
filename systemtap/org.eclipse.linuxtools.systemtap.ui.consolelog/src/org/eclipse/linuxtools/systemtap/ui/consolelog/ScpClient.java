package org.eclipse.linuxtools.systemtap.ui.consolelog;

import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;

import org.eclipse.linuxtools.systemtap.ui.consolelog.dialogs.ErrorMessage;
import org.eclipse.linuxtools.systemtap.ui.consolelog.internal.ConsoleLogPlugin;
import org.eclipse.linuxtools.systemtap.ui.consolelog.preferences.ConsoleLogPreferenceConstants;


import java.io.*;

@SuppressWarnings("deprecation")
public class ScpClient {
	
	private Session session;
  
	public ScpClient() throws Exception {
	//public static void main(String[] arg){
     
  
      String user=ConsoleLogPlugin.getDefault().getPluginPreferences().getString(ConsoleLogPreferenceConstants.SCP_USER);
      String host=ConsoleLogPlugin.getDefault().getPluginPreferences().getString(ConsoleLogPreferenceConstants.HOST_NAME);
      
      
      //System.out.println(lfile + " " + rfile);

      try{
      JSch jsch=new JSch();
      
      session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      //UserInfo ui=new MyUserInfo();
      //session.setUserInfo(ui);
      session.setPassword(ConsoleLogPlugin.getDefault().getPluginPreferences().getString(ConsoleLogPreferenceConstants.SCP_PASSWORD));
      java.util.Properties config = new java.util.Properties();
                      config.put("StrictHostKeyChecking", "no");
                      session.setConfig(config); 
      session.connect();
      }catch(Exception e)
      {
    	  e.printStackTrace(System.err);
    	  new ErrorMessage("Error in connection", "File Transfer failed.\n See stderr for more details").open();
    	  throw e;
      }
    }

    public void transfer(String fromFile, String toFile) throws Exception {
      // exec 'scp -t rfile' remotely
    	FileInputStream fis=null;	
      String rfile=toFile;
      String lfile=fromFile;
      String command="scp -t "+rfile;
      try {
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out=channel.getOutputStream();
      InputStream in=channel.getInputStream();

      channel.connect();
      

      if(checkAck(in)!=0){
    	  System.out.println("err");
      }

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize=(new File(lfile)).length();
      command="C0644 "+filesize+" ";
      if(lfile.lastIndexOf('/')>0){
        command+=lfile.substring(lfile.lastIndexOf('/')+1);
      }
      else{
        command+=lfile;
      }
      command+="\n";
      
      out.write(command.getBytes()); out.flush();
      if(checkAck(in)!=0){
    	  System.out.println("err");
      }
      


      // send a content of lfile
      fis=new FileInputStream(lfile);
      byte[] buf=new byte[1024];
      while(true){
        int len=fis.read(buf, 0, buf.length);
	if(len<=0) break;
        out.write(buf, 0, len); //out.flush();

      }
      fis.close();
      fis=null;
      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();
      if(checkAck(in)!=0){
	System.out.println("err");
      }
      out.close();

      channel.disconnect();
      session.disconnect();
      
    }
    catch(Exception e){
      try{if(fis!=null)fis.close();}catch(Exception ee){}
      throw e;
    }
  }

  static int checkAck(InputStream in) throws IOException{
    int b=in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if(b==0) return b;
    if(b==-1) return b;

    if(b==1 || b==2){
      StringBuffer sb=new StringBuffer();
      int c;
      do {
	c=in.read();
	sb.append((char)c);
      }
      while(c!='\n');
      if(b==1){ // error
	//System.out.print(sb.toString());
      }
      if(b==2){ // fatal error
	//System.out.print(sb.toString());
      }
    }
    return b;
  }

  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null, 
             str,
             "Warning", 
             JOptionPane.DEFAULT_OPTION, 
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
       return foo==0;
    }
  
    String passwd;
    JTextField passwordField=(JTextField)new JPasswordField(20);

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){
      Object[] ob={passwordField}; 
      int result=
	  JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
	passwd=passwordField.getText();
	return true;
      }
      else{ return false; }
    }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
    final GridBagConstraints gbc = 
      new GridBagConstraints(0,0,1,1,1,1,
                             GridBagConstraints.NORTHWEST,
                             GridBagConstraints.NONE,
                             new Insets(0,0,0,0),0,0);
    private Container panel;
    public String[] promptKeyboardInteractive(String destination,
                                              String name,
                                              String instruction,
                                              String[] prompt,
                                              boolean[] echo){
      panel = new JPanel();
      panel.setLayout(new GridBagLayout());

      gbc.weightx = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx = 0;
      panel.add(new JLabel(instruction), gbc);
      gbc.gridy++;

      gbc.gridwidth = GridBagConstraints.RELATIVE;

      JTextField[] texts=new JTextField[prompt.length];
      for(int i=0; i<prompt.length; i++){
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(new JLabel(prompt[i]),gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        if(echo[i]){
          texts[i]=new JTextField(20);
        }
        else{
          texts[i]=new JPasswordField(20);
        }
        panel.add(texts[i], gbc);
        gbc.gridy++;
      }

      if(JOptionPane.showConfirmDialog(null, panel, 
                                       destination+": "+name,
                                       JOptionPane.OK_CANCEL_OPTION,
                                       JOptionPane.QUESTION_MESSAGE)
         ==JOptionPane.OK_OPTION){
        String[] response=new String[prompt.length];
        for(int i=0; i<prompt.length; i++){
          response[i]=texts[i].getText();
        }
	return response;
      }
      else{
        return null;  // cancel
      }
    }
  }
}