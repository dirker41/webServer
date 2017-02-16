import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;



public class SocketServer implements Runnable { 
  private int port; 
  private ServerSocket ss; 
  private static boolean debug = true ;
  
  
  

  public SocketServer(int port) throws IOException { /////////////////////////////////
  
  
  
    
    
    this.port = port; 
    //建立一個ServerSocket 
    this.ss = new ServerSocket(port); 
    System.out.println( "Start webserver...");
    run();
  } //construction

  public void run() {////////////////////////////////////////////////////////////
        try {

            //
            // 1, 等待一個新的連接請求(Request).
            //

            Socket sk = this.ss.accept();// listen socket 

            //
            // 2, 開新Thread處理新連接請求.
            //
            
            //let web server can deal with multiple request
            //if have not just accept one packet

            Thread task = new Thread(this); //this = SimpleWebServer //run end == thread end
            task.start();

            //
            // 3, 處理請求內容.
            
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(sk.getInputStream()));
                DataOutputStream os = new DataOutputStream(sk.getOutputStream());
                

                handleRequest(sk, reader, os);
            }
            finally {
                sk.close();
            }
        } catch (Exception e) {
        }
    } //run() end
  ///////////////////////////////////
  /////////////////////////////////
  

  
  void handleRequest (Socket s, BufferedReader reader, DataOutputStream os) throws Exception {

        try { //try1

            //
            // 1, 讀取HTTP Header字串(以一個空行作結束)
            //
            
            int contentlength = 0 ;
            
            String request = "";

            while (true) {
            
                String line = reader.readLine();
                
                String line1 = line;
                line1 = java.net.URLDecoder.decode(line1, "UTF-8"); //轉碼動作
                
                
                
                if ( line1.length() > 16 && 
                     line1.substring(0,16).equals("Content-Length: ") ) {
			
			contentlength = Integer.valueOf( line.substring(16,line.length() ) ) ;
			
			
                }
                
                if (null == line) {
                    break;
                }

                request += line + "\r\n";
                if (0 == line.length()) {
                    break;
                }
            }
            
            //System.out.println( contentlength );
            
            BufferedReader content = reader;

            //System.out.println(s.getLocalAddress()); client ip address 
            System.out.println( "The Request is :================================\r\n" +
                              request+ 
                              "\r\nEnd Request ====================================") ;
            System.out.println( "The Content is :================================\r\n" +
                              content+ 
                              "\r\nEnd content ====================================") ;
                              

            //
            // 2, 解出請求的資源路徑(或包含?query字串)
            //
            String method = getMethod(request);
            method = java.net.URLDecoder.decode(method, "UTF-8"); //轉碼動作
            
            String path = getPath(request);
            path = java.net.URLDecoder.decode(path, "UTF-8"); //轉碼動作
            
            if ( !method.equals("GET") &&  // I can not handle another request 
                 !method.equals("POST") &&
                 !method.equals("HEAD") &&
                 !method.equals("PUT") &&
                 !method.equals("DELETE") ) {
              error400( os ) ;
              return ;
            } // if bad request 
                 
            
            String version = getVersion(request);
            version = java.net.URLDecoder.decode(version, "UTF-8"); //轉碼動作
            
            if( !version.equals( "1.0" ) &&
              !version.equals( "1.1" )      ) {
              error505( os ) ;
              return ;
            } // version error
            
            
            if ( !debug ) {

            System.out.print("\"" + method +"\"");
            
            
            System.out.print(" \"" + path +"\"");
            
            //String version = getVersion(request);
            //version = java.net.URLDecoder.decode(version, "UTF-8"); //轉碼動作
            
            
            System.out.println(" \"" + version +"\"");
            }  // debug mod
            
            //
            // 3, 處理請求的資源. (測試: 只處理Homepage的請求)
            //
            
            
            //getHeaderContent( request , "" ) ;
            
            
            
            // System.out.println("\"" + getHeaderContent( request , "If-Modified-Since:" ) +"\""); /////////////////多餘的
            
            
            if ( method.equals("GET") ) {
              
              if ( hadCookie( request ) ) {
                
                handleCookie( request, os ) ;
                return ;
              } // if
              
              else if ( conditionGet( request ) ){
                handleContionGet( request , os ) ;
                //GET1( request, os ) ;
                return ;
              } // conditionGet
              
              GET1( request, os ) ;
              return ;
            } // if GET
            
            if ( method.equals("POST") ) {
              POST1( request, getContent(reader,contentlength) , os ) ;
              return ;
            } // if POST

            if ( method.equals("HEAD") ) {
              HEAD1( request, os ) ;
              return ;
            }
            if ( method.equals("PUT") ) {
              PUT1( request, getContent(reader,contentlength) ,os ) ;
              return ;
            }
            if ( method.equals("DELETE") ) {
              DELETE1( request, os ) ;
              return ;
            }


            error404( os ) ;


        } catch (Exception e) { //try1 catch
            error500( os ) ;
        }
    } //handlerequest()/////////////
    
    
    void GET1( String request , DataOutputStream os ) throws Exception {
      try{
        
        
        
        
        
        
        String path = getPath(request);
        path = java.net.URLDecoder.decode(path, "UTF-8");
        
        String returnHeader = "" ;
        String packet = "";
        // System.out.println("\""+path+"\"");
  
        if ( path.endsWith("/")
             // || "/index.txt".endsWith(path) ) 
             ) 
        {
           
           if ( path.endsWith("/") ) packet = getPage( path + "index.txt" ) ;
           else packet = getPage( path ) ;
           
           
           
           
        } // if
        else if ( path.length() > 5 && 
                  path.substring(0,5).equals("/usr/") ) {
          packet = getPage( path );
          
          
          if ( packet.equals("-1") &&
               path.startsWith( "/usr/a" ) )  { 
            error301a( os ) ;
            return ;
          } // if
          if (packet.equals("-1") )  { 
            error301( os ) ;
            return ;
          } // if
          
          
          
        } // else if 
        else if (  path.length() > 11 &&
                   "/login/?id=".equals(path.substring(0,11) ) ) {
          packet=getPage( "/login.txt" ) ;
          
          
          packet=dealID( request , packet );
        
          
          
          if ( packet.equals("-1") ) {
            error404( os );
            return ;
          }  // if page not exist
       
          String id = getId(request) ;
          // System.out.println( "!!!!!!!!!!!!!!!!!" + id +"!!!!!!!!!!!!!!!!!!") ;
          
          packet = "Set-Cookie: id=" + id + "\r\n" + packet;
          
          
          
        } // else if 
        else if ( !path.startsWith( "/usr/" ) && 
                  !path.startsWith( "/index" ) ) {
          error404( os );
          
          
          return ;
          
        } // else if
        
        
          
          OK200( packet, os ) ;
          


          
        } // try 
        catch (Exception e) { 
            error500( os ) ;
        }
      }  //GET1() end //////////////////////////////////////////////////////////////////
      
      void POST1( String request , String content , DataOutputStream os ) throws Exception {
        try{
          
          if ( content.length() == 0 ) {
            GET1( request , os ) ;
            return ;
          }
          
          String p1="";
          String p2="";
          
          int start = 0 ;
          int end = 0;
          end = request.indexOf("HTTP/");
          p1 = request.substring(0 , end ).trim() ;
          
          start = end ;
          end = request.length();
          //System.out.println( request.length() );
          p2 = request.substring( start,end);
          
          //System.out.println( p2 ) ;
          
          request = p1 + "?" + content + " " + p2 ;
          
          // System.out.println( request ) ;
          
          
          
          
          
          
          
          
          GET1( request , os ) ;
          
        }
        catch (Exception e) { 
            error500( os ) ;
        }
      }  //POST1() end //////////////////////////////////////////////////////////////////
      
      
      void HEAD1( String request , DataOutputStream os ) throws Exception {
        try{
          String path = getPath(request);
          path = java.net.URLDecoder.decode(path, "UTF-8");
            if ("/".equals(path) || 
                "/index.txt".equals(path) ) {
              OK200( os ) ;
              return;
          }
          
          else {
            String page = getPage( path ) ;
            if ( page.equals("-1") ) {
              error404(os);
              return ;
            }
            else{
              OK200( os ) ;
              return;
            }
          } // 

            //
            // TODO: 處理其它請求.
            //

           // error404( os ) ;
        }
        catch (Exception e) { 
            error500( os ) ;
        }
      }  //HEAD1() end //////////////////////////////////////////////////////////////////
      
      
      
      
      void PUT1( String request , String content ,DataOutputStream os ) throws Exception {
        try{
          String version = getVersion(request);
          version = java.net.URLDecoder.decode(version, "UTF-8");

          if( version.equals( "1.0" ) ) {
            error505( os ) ;
            return ;
          } // if
          
          String path = getPath(request);

          path = java.net.URLDecoder.decode(path, "UTF-8");
          
          if (  path.length() < 6 ||
                !path.substring(0,5).equals("/usr/") ) { //放在USR之外都不允許
                         
            GET1( request, os ) ;//if exists print it 
                
            return;
          } //if
          else if ( path.substring(5,path.length() ).indexOf("/") != -1 ) { //不允許在/USR再創目錄
            error400(os) ;
            return;
          } //else if
          else if ( path.equals("/usr/-1") ) { //創-1根本是玩我阿
                    
            error400( os ) ;
            return ;
          } //else if
          else{ // 應該都OK了吧= =?
            
            
            path= path.substring(1,path.length() ) ;
            
            File f = new File( path ) ;
            if ( !f.exists() ) f.createNewFile() ;
            FileWriter fp = new FileWriter( f ) ;
           
            fp.write( content ) ;
            fp.flush();
            fp.close();
           
            GET1( request , os ) ;
            return ;
          } // ekse

            //
            // TODO: 處理其它請求.
            //

          //error404( os ) ;
        }
        catch (Exception e) { 
            error500( os ) ;
        }
      }  //PUT1() end //////////////////////////////////////////////////////////////////
      
      
       void DELETE1( String request , DataOutputStream os ) throws Exception {
         try{
          String version = getVersion(request);
          version = java.net.URLDecoder.decode(version, "UTF-8");

          if( version.equals( "1.0" ) ) {
            error505( os ) ;
            return ;
          }

          String path = getPath(request);
          path = java.net.URLDecoder.decode(path, "UTF-8");

          if (  path.length() < 6 ||
                !path.substring(0,5).equals("/usr/") ) { //delete USR之外都不允許
                      
            GET1( request, os ) ;//if exists print it 
            return;
          } 
          else {
            path= path.substring(1,path.length() ) ;
            File f = new File( path ) ;
            
            if ( f.exists() ) { //if exists,  delete
            f.delete() ;
            OK200( os ) ;
            return ;
            } //if
            else
              error404( os ) ;
              
            return ;
          } // else

            //
            // TODO: 處理其它請求.
            //

         // error404( os ) ;
        }
        catch (Exception e) { 
            error500( os ) ;
        }
      }  //DELETE() end //////////////////////////////////////////////////////////////////
      
      
    boolean conditionGet( String request ){
      if ( request.indexOf("If-Match: ") != -1 ) return true ;
      if ( request.indexOf("If-None-Match: ") != -1 ) return true ;
      if ( request.indexOf("If-Modified-Since: ") != -1 ) return true ;
      if ( request.indexOf("If-Unmodified-Since: ") != -1 ) return true ;
      if ( request.indexOf("If-Range: ") != -1 ) return true ;
      

      return false ;
    }///congitionGet()///////////////////////
    
    boolean hadCookie( String request ){
        
      // if ( request.indexOf("Cookie: ") == -1 ) return false ;
      
      if ( request.indexOf("Cookie: ") != -1 && 
           getPath( request ).equals("/login/") ) return true ;
           // had cookie and path is /login/
           

      return false ;
    } //hadCookie()///////////////////////
    
    void handleCookie( String request , DataOutputStream os ) throws Exception {
      try{
        
        String content = "" ;
        // System.out.println( "22222222\n" + "22222222\n" ) ;
        
        content = getHeaderContent( request ,"\r\nCookie: ") ;
        
        // System.out.println( "22222222\n"+ content + "22222222\n" ) ;
        
        String path = getPath( request ) ;
        
        
        
        
        String contentValue = content.substring( content.indexOf( "=" )+1 ,content.length() ) ;
        
        
        path = path + "?" + content +"&passwd=" + contentValue;
        
        request = "GET " + path + " HTTP/1.1" + request.substring( request.indexOf("\r\n") , request.length() ) ;
        
        System.out.println( "22222222\n"+ request + "22222222\n" ) ;
        
        
        String packet=getPage( "/login.txt" ) ;
          
          
        packet=dealID( request , packet );
        
        OK200( packet, os ) ;
          
        
        // System.out.println( "22222222\n"+ path + "\n22222222\n" ) ;
        
        // GET1( request,os ) ;
        
        
        
      } catch( Exception e ) {
        error505( os );
      } // catch()
    } // handleCookie()
    
      
    void handleContionGet( String request , DataOutputStream os ) throws Exception {
      try{
        String path = getPath(request);
        path = java.net.URLDecoder.decode(path, "UTF-8");
        File f = null ;
        // 開檔
        if ( path.equals("/") || 
             path.equals("/index.txt") ) 
          f = new File( "index.txt" ) ; 
          
        else if ( path.length() > 5 && 
                  path.substring(0,5).equals("/usr/") ) 
          f = new File( path.substring( 1,path.length() ) ) ;
        else if ( path.endsWith( "/" ) ) {
          f = new File( path + "index.txt" ) ; //
        }
                          
        // 開檔 NED
                
        if ( f == null ) { //找不到檔 就閃人
          error404( os ) ; 
          return ;
        }
        
        String content1=""; //modified
        String content2=""; //Etag 
        
        DateFormat dateFormat = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);//respect in English
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                
        // get headder content//////////////////////////////////
        if ( request.indexOf("\r\nIf-Modified-Since: ") != -1 ) // if this header exists , get it ;
          content1 = getHeaderContent( request ,"If-Modified-Since: ") ;
        else if ( request.indexOf("\r\nIf-Unmodified-Since: ") != -1 )
          content1 = getHeaderContent( request ,"If-Unmodified-Since: ") ;
                
                
        if ( request.indexOf("\r\nIf-None-Match: ") != -1 ) // if this header exists , get it ;
          content2 = getHeaderContent( request ,"If-None-Match: ") ;
        else if ( request.indexOf("\r\nIf-Match: ") != -1 )
          content2 = getHeaderContent( request ,"If-Match: ") ;
        // get header content end ////////////////////////////////////
        // if header not exists , content is null string /////////////////
                
        Date file_date = new Date ( f.lastModified() );
        String fEtag = getEtag( f ) ;
                
        Date request_date = null;
                
        
                
        if ( !content1.equals("") ) request_date = dateFormat.parse(content1);
                
        if ( request.indexOf("\r\nIf-Range: ") != -1 &&
             request.indexOf("\r\nRange: ") != -1 ) {
          String If_Range = "" ;
          String Range = "" ;
          
          If_Range = getHeaderContent( request ,"If-Range: ") ;
          Range = getHeaderContent( request ,"\nRange: ") ;
          
          //System.out.println( "\"" +If_Range +"\"" ) ;
          //System.out.println( "\"" + Range + "\"" ) ;
                
          if ( If_Range.equals( fEtag ) || 
               If_Range.equals( dateFormat.format( file_date ) )  ) {
            // If-Range the same
            long begin = 0 ;
            long end = 0 ;
          
            if ( Range.substring(0,1).equals("-") ) { //EX -500 means last 500 bytes
              end = f.length() ; //last
            
              try {
                begin = 
                f.length() - Integer.parseInt( Range.substring( 1,Range.length() ) ) ;
                // last - 500 ;
              }catch (Exception e) { 
              } // catch()
            } //if Range.substring(0,1).equals("-")
            else if ( Range.substring(Range.length()-1,Range.length()).equals("-") ) { 
            // EX 500- means 500bytes ~ last;
              end = f.length() ;
              try {
                begin = Integer.parseInt( Range.substring( 0,Range.length()-1 ) ) ;
                // 500 ;
              }catch (Exception e) { 
              } // catch()
            }//else if Range.substring(Range.length()-1,Range.length()).equals("-") ) { 
            else {
              int mid = Range.indexOf("-") ;
            
              try {
                begin = Integer.parseInt( Range.substring( 0,mid ) ) ;
                end = Integer.parseInt( Range.substring( mid + 1 ,Range.length() ) ) + 1 ;
              }catch (Exception e) { 
              } // catch()
            } //else // get range end
          

            DataInputStream fp = new DataInputStream ( new FileInputStream( f ) );
            String page = "" ;
            long length = f.length() ;
            int i = 0 ;
            try{
              while( i < length  ) {
                byte b = fp.readByte() ;
                if ( i >= begin && i < end )
                page += ( char ) b ;
                i++;
              } // while
            }catch (Exception e) {
            } // catch()
          
            fp.close();
          
            //System.out.println( page ) ;
            //System.out.println( begin + " "  + end  ) ;
          
            Date date = new Date( f.lastModified() ) ;
            String header = "" ;
            header = header.concat ( "Last-modified: " + dateFormat.format( date ) + "\r\n" ) ;
            header = header.concat ( "Etag: " + fEtag +"\r\n" ) ;
            header = header.concat ( "Content-range: bytes " + begin + " " + end + "/" + f.length() + "\r\n" ) ;
          
            OK206( header+"\r\n"+page , request ,os ) ;
            return ;
          } // If-Range the same
        } // if-Range and Range header is exist ;
                
                
                
                
                //error 412
                if ( request_date != null && //header exist
                     request.indexOf("If-Unmodified-Since: ") != -1 && // header  If-None-Match: 
                     request_date.getTime()/1000 < file_date.getTime()/1000 ) { //ignore last 3 digital
                     //DATE < 最後修改日期 412 Precondition Failed
                
                     error412( os ) ;
                     return ;
                } //if
                
                if ( 	request.indexOf("If-Match: ") != -1 && 
			!fEtag.equals( content2 ) ) {
				
			error412( os ) ;
                     	return ;
		} //if 
                //error 412 end
                
                
                // System.out.println( "maybe Error 304???????????????????") ;
                
                // error 304
                if ( request.indexOf("If-Modified-Since: ") != -1 && 
                     request_date != null && 
                     request_date.getTime()/1000 >= file_date.getTime()/1000 ) {
			//最後修改日期 <= DATE 304 Not Modified (only head)
			if ( request.indexOf("If-None-Match: ") != -1 ) {
				if( fEtag.equals( content2 ) || 
				    content2.equals("*") ) {
					error304(os);
					return ;
				} 
			} //if If-None-Match: header exist 
			
			
                } // if "If-Modified-Since: " header exist ;
                // error 304 end
                
                
                if ( request.indexOf("If-Modified-Since: ") != -1 && 
                     request_date != null && 
                     request_date.getTime()/1000 >= file_date.getTime()/1000 ) {
                       
                  error304(os);
                  return ;
                } // if 
                
                
                if ( request.indexOf("If-None-Match: ") != -1 ) {
                  if ( fEtag.equals( content2 ) || 
                       content2.equals("*") ) {
                    error304(os);
                    return ;
                 } 
                } //if If-None-Match: header exist 
                
               
           GET1( request ,os ) ;
               
               
        }catch (Exception e) {
            error412( os ) ;
        }
    }   //handleContionGet()  end /////////////
        /////////////////////////////
         /////////////////////////////
      
      
    String getMethod(String request) {

        //
        // 截取method
        //


        int endStart = request.indexOf(" /");
        if (endStart < 0) {
            return null;
        }

        return request.substring(0, endStart).trim();
    }
    

    String getPath(String request) {

        //
        // 截取介於"method" ... "HTTP/"之間的路徑
        //

        int beginStart = request.indexOf("/");
        if (beginStart < 0) {
            return null;
        }

        int endStart = request.indexOf("HTTP/");
        if (endStart < 0) {
            return null;
        }

        return request.substring(beginStart, endStart).trim();
    }  //getpath() end
    /////////////////////////////////
    ////////////////////////////////
    
    String getId(String request) {

        //
        // 截取Id
        //


        int begin = request.indexOf("/?id=") ;
        if ( begin < 0) return null ;
        
        begin = begin + "/?id=".length();
        
        int end = 0;
        
        end = request.indexOf("&passwd=") ;
        if ( end < 0 ) 
        end = request.indexOf( "HTTP/" );
        

        return request.substring(begin, end).trim();
    }
    
    String getVersion(String request) {

        //
        // 截取第一行 的版本
        //

        int beginStart = request.indexOf("HTTP/");
        if (beginStart < 0) {
            return null;
        }

        return request.substring( beginStart+5, beginStart+8).trim();
    }  //getVersion() end
    /////////////////////////////////
    ////////////////////////////////
    
    
     String getContent( BufferedReader reader , int num ) {

        //
        // 截取Content
        //
        String content = "";
        
        
        
        for ( int i = 0 ; i < num ; i++ ) {
          try {
            char ch = 0x00;
            int code ;
            code = reader.read() ;
            
            if ( code == -1 ) break ;
            
            ch = ( char ) code ;
            content += ch ;
            // System.out.println( i ) ;
            // num -- ;
          }catch (Exception e) { //try catch
          ;
          } // catch()

        } // for
        
        
        content = content.trim();
        
        // System.out.println( "Get content end" ) ;
        
        return content ;
        
    }  //getContent() end
    /////////////////////////////////
    ////////////////////////////////
    
    String getHeaderContent( String request ,String header ){
        
        // the header must begin at "/r/n"
	
	if ( request.indexOf( header ) == -1 ) return "null" ; //can not find header
	
	int begin = 0 ;
	int end = 0 ;
	
	begin = request.indexOf( header ) + header.length(); // 123: 4456\r\n;
	
	end = request.indexOf( "\r\n" , request.indexOf( header )+1 ) ; // 
	
	if ( begin == -1 || end == -1 ) return "null" ;
	
	String content = request.substring( begin, end ).trim() ;
	
	if ( content.indexOf(";") == -1 ) return content ;
	
	content = content.substring( 0 , content.indexOf(";") ).trim() ;
	
	return content ;
	
	
    } //getHeaderContent()//////////////
    ///////////////////////////////////
    ///////////////////////////////////
    
    String getEtag( File f ) {
	if ( f == null ) return "0x0";
	String Etag = "" ;
	
	//long date = f.lastModified() ;
	
	//int length = f.length() ;
	
	return "\"" + numToHex( f.lastModified()  ) + "x" + numToHex( f.length() ) +"\"";
	
	
	
    } ////////getEtag() ///////////////////
    ///////////////////////////////////////
    ////////////////////////////////////////
    
    String getPage( String page ) {
      // 從電腦中去找PAGE
      String page1 = "";
      File file = null;
      String header = "" ;
      
      
      
      
      
      if ( page.endsWith("/index.txt") ) {
        try{
          file = new File( page.substring( 1,page.length() ) ) ;
          
          
          if( !file.exists() ) return "-1" ; 
          
          DataInputStream fp = new DataInputStream ( new FileInputStream( file ) );
          
          int i = 0 ;
          
          while( i < file.length() ) {
            byte b = fp.readByte();
            if ( b == -1 ) break ; //其實不會RETURN -1 但是CATCH抓到錯誤 所以程式繼續RUN
            page1 += ( char ) b ;
            
            i++ ;
          } //while
          
          
          
          page1 = "\r\n" + page1 ;
          
          page1 = "Content-Type: text/plain\r\n" + page1;
          // page1 = "Content-Length: " + file.length() + "\r\n" + page1; 
          // content-Length is not necessary
          
          
          
          
          fp.close();
          
        } catch (Exception e) { // try catch
        
          
          
        } // catch
                
      } //if page.endsWith("/index.txt") ) 
      else {
        
        try{
          file = new File( page.substring( 1,page.length() ) ) ;
          
          
          if( !file.exists() ) return "-1" ; 
          
          DataInputStream fp = new DataInputStream ( new FileInputStream( file ) );
          
          int i = 0 ;
          
          while( i < file.length() ) {
            byte b = fp.readByte();
            if ( b == -1 ) break ; //其實不會RETURN -1 但是CATCH抓到錯誤 所以程式繼續RUN
            page1 += ( char ) b ;
            
            i++ ;
          } //while
          
          
          
          page1 = "\r\n" + page1 ;
          
          page1 = "Content-Type: text/plain\r\n" + page1;
          // page1 = "Content-Length: " + file.length() + "\r\n" + page1;
          
          
          
          
          fp.close();
          
        } catch (Exception e) { // try catch
        
          
          
        } // catch
      } // else 
      
      
      long date = file.lastModified() ;
      Date date1 = new Date( date ) ;
      DateFormat dateFormat =  
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US); //respect in English
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      page1 = "Last-Modified: " + dateFormat.format( date1) + "\r\n" + page1 ;
      
      String Etag = getEtag( file ) ;
      page1 = "Etag: " + Etag + "\r\n" + page1 ;
      
      //this.mHeader += "Content-Type: text/plain\r\n" ;
      //this.mHeader += "Content-Length: " + page1.length() + "\r\n" ;
      //this.mPage   += page1 ;
                     
      
      // this.mLength = page1.length();
      
      
      
      // System.out.println( page1 ) ;
      
      return page1;
    } //getPage()////////////////////////
    ////////////////////////////////////
    //////////////////////////////////////
    
    
    
    String dealID( String request, String page ) {
	
	String ID = "";
	int start = 0 ;
	int end = 0 ;
	
	start = request.indexOf( "/?id=" ) + 5 ;
	end = request.indexOf( "&" ) ;
	if ( end == -1 ) return "-1" ; //nopasswd
	
	ID = request.substring( start, end ) ;
	if( ID.length() > 8 ) return "-1"; //ID to long
	
	String passwd ="";
	start = request.indexOf( "&passwd" ) + 8 ;
	end = request.indexOf( "HTTP/" ) ;
	
	passwd=request.substring( start,end ).trim() ;
	
	
	// System.out.println( "!" + page + "!" ) ;
	// System.out.println( "!" + ID + "!" ) ;
	// System.out.println( "!" + passwd + "!" ) ;
	
	if ( !ID.equals(passwd) ) return "-1" ; // ID != passwd
	
	String p1 ="";
	String p2 ="";
	
	start = page.indexOf("Your ID is :")+ 12 ;
	p1 = page.substring( 0 , start ) ;
	p2 = page.substring( start, page.length() );
	
	page = p1 + ID + p2 ; 
	// System.out.println(page);
	
	
	
	return page ;
    }////dealID()//////////////////////////////
    ///////////////////////////////////////////
    
    
    String numToHex( long num ) {
	
	long q = 0 ;
	
	String hex = "" ;
	
	
	
	while( num > 0 ) {
		q = num%16 ;
		char ch = '0' ;
		
		if ( q == 10 ) ch = 'A' ;
		if ( q == 11 ) ch = 'B' ;
		if ( q == 12 ) ch = 'C' ;
		if ( q == 13 ) ch = 'D' ;
		if ( q == 14 ) ch = 'E' ;
		if ( q == 15 ) ch = 'F' ;
		
		if ( q > 9 ) hex = ch + hex;
		else hex = q + hex;
		
		num = num/16;
	}// while
	
	
	return hex ;
    } ///numToHex()//////////////////
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
    
    String numToHex( int num ) {
	
	int q = 0 ;
	
	String hex = "" ;
	
	
	
	while( num > 0 ) {
		q = num%16 ;
		char ch = '0' ;
		
		if ( q == 10 ) ch = 'A' ;
		if ( q == 11 ) ch = 'B' ;
		if ( q == 12 ) ch = 'C' ;
		if ( q == 13 ) ch = 'D' ;
		if ( q == 14 ) ch = 'E' ;
		if ( q == 15 ) ch = 'F' ;
		
		if ( q > 9 ) hex += ch ;
		else hex += q ;
		
		num = num/16;
	}// while
	
	
	return hex ;
    } ///numToHex()//////////////////
    ////////////////////////////////
    ////////////////////////////////
    ////////////////////////////////
      
      
      ////////////////////////////////////////////////////////////
      //                     stutsCODE                          //
      ////////////////////////////////////////////////////////////
      void OK200 ( DataOutputStream os ) throws Exception  {
        try {
          os.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
          return;
                 
        } // try
        catch (Exception e) { 
            error500( os ) ;
        } // catch
      } // OK200
      
      
      void OK200 ( String packet,DataOutputStream os ) throws Exception  {
        try {
                 System.out.println(
                   "111111111 Response 11111111\n" +
                   "HTTP/1.1 200 OK\r\n" + 
                   packet +
                   "\n111111111 Response End 11111111"
                 ) ;
                 
                 
                 
                 
                 
                 
                 os.writeBytes(
                     "HTTP/1.1 200 OK\r\n" + 
                     packet );
                 
                 
      }
      catch (Exception e) { 
            error500( os ) ;
      }
      } //OK200() end
      
      void OK206 ( String str ,  String request ,DataOutputStream os ) throws Exception  {
	try {
		String path = getPath(request);
          	path = java.net.URLDecoder.decode(path, "UTF-8");
          	
          	
                 os.writeBytes(
                     "HTTP/1.1 206 Partial Content\r\nContent-Type: text/html\r\n" +
                     str );
                 
		
		
      }
      catch (Exception e) { 
            error500( os ) ;
      }
      } //OK206() end
    
      
      void error500 ( DataOutputStream os ) throws Exception  {
	os.writeBytes("HTTP/1.0 500 Internal Server Error\r\n\r\n" + 
	"500 Internal Server Error" );
      }
      
      void error404 ( DataOutputStream os ) throws Exception  {
                System.out.println("HTTP/1.1 404 File Not Found\r\n\r\n");
	os.writeBytes("HTTP/1.1 404 File Not Found\r\n\r\n" + 
	"404 File Not Found" );
      }
      
      void error505 ( DataOutputStream os ) throws Exception  {
	os.writeBytes("HTTP/1.0 505 HTTP Version Not Supported\r\n\r\n" +
	"HTTP Version Not Supported" );
      }
      
      void error400 ( DataOutputStream os ) throws Exception  {
	os.writeBytes("HTTP/1.0 400 Bad Request\r\n\r\n" +
	"400 Bad Request" );
      }
      
      void error301 ( DataOutputStream os ) throws Exception  {
	System.out.println( "HTTP/1.1 301 Moved Permanently\r\nLocation: http://127.0.0.1/\r\n\r\n" ) ;
	// make admit realize that server is not really sent OK200
	os.writeBytes("HTTP/1.1 301 Moved Permanently\r\nLocation: http://127.0.0.1/\r\n\r\n");
      }
      
      void error301a ( DataOutputStream os ) throws Exception  {
        System.out.println( "HTTP/1.1 301a Moved Permanently\r\nhttp://127.0.0.1/\r\n\r\n" ) ;
        // make admit realize that server is not really sent OK200
        
        
          
          os.writeBytes( "HTTP/1.1 301 Moved Permanently\r\n" +
                         "Content-Type: text/html\r\n\r\n" +
                         "<html>\r\n"+
                         "<head>\r\n"+
                         "<title>Moved</title>\r\n"+
                         "</head>\r\n"+
                         "<body>\r\n"+
                         "the page is not exists<br>" + 
                         "<a href=\"http://127.0.0.1/\">click here to index</a>\r\n"+
                         "</body>\r\n"+
                         "</html>" );
                         
                         // html hyperlink sample 
      }
      
      void error304 ( DataOutputStream os ) throws Exception  {
	System.out.println( "HTTP/1.1 304 Not Modified\r\n\r\n" ) ;
	// make admit realize that server is not really sent OK200
	os.writeBytes("HTTP/1.1 304 Not Modified\r\n\r\n");
      }

      void error412 ( DataOutputStream os ) throws Exception  {
                // conditional get error 
                // it means the server can not deal with the condition get request 
                // 
	os.writeBytes("HTTP/1.1 412 Precondition Failed\r\n\r\n");
      }
      
      
      
      ////////////////////////////////////////////////////////////
      //                     stutsCODE                          //
      ////////////////////////////////////////////////////////////
      
      
  public static void main(String args[]) throws Exception { /////////////////////////////////////////
    // runable要new一個Thread,再把runnable置入 
    
    new SocketServer(80); //web socket
    
  } // main() end
}