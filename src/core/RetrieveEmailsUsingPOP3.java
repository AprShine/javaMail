/**
 * 
 */
package core;

import util.ConnectStatus;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
/**
 * @author AprShine
 *
 */
public class RetrieveEmailsUsingPOP3 {
	
	public void getCompleteInbox(String host, String port, String userName, String password, String secureCon) throws IOException {
        Properties properties = new Properties();
 
    
        //---------- Server Setting---------------
        properties.put("mail.pop3.host", host);
        properties.put("mail.pop3.port", port);
        if(secureCon.equalsIgnoreCase("ssl")){
        	properties.put("mail.smtp.ssl.enable", "true");
        }else{
        	properties.put("mail.smtp.ssl.enable", "false");
        }
        //---------- SSL setting------------------
        properties.setProperty("mail.pop3.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.pop3.socketFactory.fallback", "false");
        properties.setProperty("mail.pop3.socketFactory.port", String.valueOf(port));
        Session session = Session.getDefaultInstance(properties);
        //----------------------------------------
      try {
            // connects to the message store
        	System.out.println("Connecting please wait....");
            Store store = session.getStore("pop3");
            store.connect(userName, password);
            System.out.println("Connected to mail via "+host);
            System.out.println();
            // opens the inbox folder
            System.out.println("Getting INBOX..");
            System.out.println();
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);
 
            // fetches new messages from server
            Message[] arrayMessages = folderInbox.getMessages();
            DataBuffer.inboxMail=arrayMessages;
            System.out.println("You have "+arrayMessages.length+" mails in your INBOX");
             
            for (int i = 0; i < arrayMessages.length ; i++) {
                Message message = arrayMessages[i];
                Address[] fromAddress = message.getFrom();
               
                String from = fromAddress[0].toString();
                Date sentdate = message.getSentDate();
                String subject= message.getSubject();
               
                String contentType = message.getContentType();
                String messageContent = "";
 
                // store attachment file name, separated by comma
                String attachFiles = "";
                
                if (contentType.contains("multipart")) {
                    // content may contain attachments
                    Multipart multiPart = (Multipart) message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            // this part is attachment
                        	 String fileName = part.getFileName();
                             attachFiles += fileName + ", ";
                             part.saveFile("./attach" + File.separator + fileName); // hard code service file storage here
                             messageContent = getText(message);  // to get message body of attached emails   
                       }
                    
                         else {
                            // this part for the message content
                            messageContent = part.getContent().toString();
                      }
                    }
                     if (attachFiles.length() > 1) {
                        attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                    }
                } else if (contentType.contains("text/plain") || contentType.contains("text/html")) {
                    Object content = message.getContent();
                    if (content != null) {
                        messageContent = content.toString();
                    }
                }
                // print out details of each message and save this data into DB here
                System.out.println("\t \n Message #" + (i + 1) + ":");
                System.out.println("\t From: " + from);
                System.out.println("\t Subject: " + subject);
                System.out.println("\t Sent Date: " + sentdate);
                System.out.println("\t Message: " + messageContent);
                System.out.println("\t Attachments: " + attachFiles);
                System.out.println();
                System.out.println("\n ------------------------------ \n");
              }
            // disconnect
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider for pop3.");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        }
	}
 /** 检验是否能够连接上服务器 */
    public static ConnectStatus getConnectionStatus(String host, String port, String userName, String password){
        //链接属性集
    	  Properties properties = new Properties();
    	  
          //---------- Server Setting---------------
          properties.put("mail.pop3.host", host);
          properties.put("mail.pop3.port", port);
          properties.put("mail.smtp.ssl.enable", "true");
          //---------- SSL setting------------------
          properties.setProperty("mail.pop3.socketFactory.class",
                  "javax.net.ssl.SSLSocketFactory");
          properties.setProperty("mail.pop3.socketFactory.fallback", "false");
          properties.setProperty("mail.pop3.socketFactory.port",
                  String.valueOf(port));
          Session session = Session.getDefaultInstance(properties);
          //----------------------------------------
        ConnectStatus isConnected;
        try {
              // connects to the message store
          	System.out.println("Connecting please wait....");
              Store store = session.getStore("pop3");
              store.connect(userName, password);
              isConnected = ConnectStatus.CONNECTED_POP3;
              System.out.println("Is Connected: "+ isConnected);
              System.out.println("Connected to mail via "+host);
        }catch (NoSuchProviderException ex) {
        	ConnectStatus ex1=ConnectStatus.NO_PROVIDER_FOR_POP3;
            System.out.println(ex1);
            return ex1;
        } catch (MessagingException ex) {
        	ConnectStatus ex2 = ConnectStatus.COULD_NOT_CONNECT;
            System.out.println(ex2);
            return ex2;
            //ex.printStackTrace();
        }
		return isConnected;
    }
    
    
    /**
     *  This method is use to handle MIME message. 
     *  a message with an attachment is represented in MIME as a multipart message. 
     *  In the simple case, the results of the Message object's getContent method will be a MimeMultipart object. 
     *  The first body part of the multipart object wil be the main text of the message. 
     *  The other body parts will be attachments. 
     * @param p
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    
 	public static String getText(Part p) throws MessagingException, IOException {
 		if (p.isMimeType("text/*")) {
 			String s = (String) p.getContent();
 			return s;
 		}

 		if (p.isMimeType("multipart/alternative")) {
 			// prefer html text over plain text
 			Multipart mp = (Multipart) p.getContent();
 			String text = null;
 			for (int i = 0; i < mp.getCount(); i++) {
 				Part bp = mp.getBodyPart(i);
 				if (bp.isMimeType("text/plain")) {
 					if (text == null)
 						text = getText(bp);
 					continue;
 				} else if (bp.isMimeType("text/html")) {
 					String s = getText(bp);
 					if (s != null)
 						return s;
 				} else {
 					return getText(bp);
 				}
 			}
 			return text;
 		} else if (p.isMimeType("multipart/*")) {
 			Multipart mp = (Multipart) p.getContent();
 			for (int i = 0; i < mp.getCount(); i++) {
 				String s = getText(mp.getBodyPart(i));
 				if (s != null)
 					return s;
 			}
 		}

 return null;
 	}
}