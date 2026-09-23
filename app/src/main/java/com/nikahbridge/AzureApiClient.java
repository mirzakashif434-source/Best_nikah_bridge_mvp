package com.nikahbridge;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class AzureApiClient {
  private static final String BASE="https://bestnikahbredge-prod-fn-dkf3ake6d8gsg7cw.eastus-01.azurewebsites.net/api";
  private static final ExecutorService EXEC=Executors.newCachedThreadPool();
  private static final ScheduledExecutorService TIMEOUTS=Executors.newSingleThreadScheduledExecutor();
  private static final long AUTH_TIMEOUT_MS=12000L;
  interface Callback{void ok(int code,String body);void err(String message);}
  interface BinaryCallback{void ok(int code,String contentType,byte[] body);void err(String message);}

  static void get(String path,Callback cb){request("GET",path,null,"application/json",cb,true);}
  static void getBytes(String path,BinaryCallback cb){
    authToken(token->{EXEC.execute(()->{
      HttpURLConnection c=null;
      try{
        c=(HttpURLConnection)new URL(BASE+path).openConnection();
        c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(25000);
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("Accept","image/*"); c.setRequestProperty("X-App-Language",LanguageManager.currentCode());
        int code=c.getResponseCode();
        InputStream in=code>=400?c.getErrorStream():c.getInputStream();
        if(code>=200&&code<300){
          ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
          while(in!=null&&(n=in.read(buf))!=-1)out.write(buf,0,n);
          if(in!=null)in.close();
          cb.ok(code,c.getContentType(),out.toByteArray());
        }else{
          StringBuilder b=new StringBuilder();
          if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String s;while((s=r.readLine())!=null)b.append(s);}
          cb.err("HTTP "+code+(b.length()==0?"":": "+b));
        }
      }catch(Exception e){cb.err(e.getMessage()==null?"NETWORK_ERROR":e.getMessage());}
      finally{if(c!=null)c.disconnect();}
    });},new Callback(){public void ok(int code,String body){} public void err(String m){cb.err(m);}});
  }
  static void post(String path,String json,Callback cb){request("POST",path,json,"application/json; charset=UTF-8",cb,true);}
  static void put(String path,String json,Callback cb){request("PUT",path,json,"application/json; charset=UTF-8",cb,true);}
  static void patch(String path,String json,Callback cb){request("PATCH",path,json,"application/json; charset=UTF-8",cb,true);}
  static void delete(String path,String json,Callback cb){request("DELETE",path,json,"application/json; charset=UTF-8",cb,true);}

  static void multipart(String path,String field,String fileName,String mime,byte[] data,String[] names,String[] values,Callback cb){
    authToken(token->{EXEC.execute(()->{
      HttpURLConnection c=null; String boundary="----BNB"+System.currentTimeMillis();
      try{
        c=(HttpURLConnection)new URL(BASE+path).openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setConnectTimeout(20000); c.setReadTimeout(30000);
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("X-App-Language",LanguageManager.currentCode());
        c.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
        try(DataOutputStream o=new DataOutputStream(c.getOutputStream())){
          if(names!=null) for(int i=0;i<names.length;i++){
            o.writeBytes("--"+boundary+"\\r\\nContent-Disposition: form-data; name=\""+names[i]+"\"\\r\\n\\r\\n");
            o.write((values[i]==null?"":values[i]).getBytes(StandardCharsets.UTF_8)); o.writeBytes("\\r\\n");
          }
          o.writeBytes("--"+boundary+"\\r\\nContent-Disposition: form-data; name=\""+field+"\"; filename=\""+fileName+"\"\\r\\nContent-Type: "+mime+"\\r\\n\\r\\n");
          o.write(data); o.writeBytes("\\r\\n--"+boundary+"--\\r\\n");
        }
        finish(c,cb);
      }catch(Exception e){cb.err(e.getMessage()==null?"NETWORK_ERROR":e.getMessage());}
      finally{if(c!=null)c.disconnect();}
    });},cb);
  }

  private static void request(String method,String path,String body,String contentType,Callback cb,boolean allowAuthRetry){
    authToken(token->{EXEC.execute(()->{
      HttpURLConnection c=null;
      try{
        c=(HttpURLConnection)new URL(BASE+path).openConnection();
        c.setRequestMethod(method); c.setConnectTimeout(15000); c.setReadTimeout(25000);
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("X-App-Language",LanguageManager.currentCode());
        if(body!=null){
          c.setDoOutput(true); c.setRequestProperty("Content-Type",contentType);
          try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}
        }
        int code=c.getResponseCode();
        InputStream in=code>=400?c.getErrorStream():c.getInputStream();
        StringBuilder response=new StringBuilder();
        if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
          String s;while((s=r.readLine())!=null)response.append(s);
        }
        if(code>=200&&code<300){
          cb.ok(code,response.toString());
        }else if(code==401&&allowAuthRetry){
          AzureAuthManager.clearCachedToken();
          request(method,path,body,contentType,cb,false);
        }else if(code==401){
          AzureAuthManager.clearCachedToken();
          cb.err("AZURE_SIGN_IN_REQUIRED");
        }else{
          cb.err("HTTP "+code+(response.length()==0?"":": "+response));
        }
      }catch(Exception e){cb.err(e.getMessage()==null?"NETWORK_ERROR":e.getMessage());}
      finally{if(c!=null)c.disconnect();}
    });},cb);
  }

  private static void authToken(java.util.function.Consumer<String> work,Callback cb){
    AtomicBoolean finished=new AtomicBoolean(false);
    Runnable timeout=()->{
      if(finished.compareAndSet(false,true)){
        cb.err("AZURE_AUTH_TIMEOUT");
      }
    };
    TIMEOUTS.schedule(timeout,AUTH_TIMEOUT_MS,TimeUnit.MILLISECONDS);

    AzureAuthManager.acquireToken(new AzureAuthManager.Callback(){
      @Override public void ok(String token){
        if(finished.compareAndSet(false,true)) work.accept(token);
      }
      @Override public void err(String message){
        if(finished.compareAndSet(false,true)) cb.err(message);
      }
    });
  }

  private static void finish(HttpURLConnection c,Callback cb)throws Exception{
    int code=c.getResponseCode();
    InputStream in=code>=400?c.getErrorStream():c.getInputStream();
    StringBuilder b=new StringBuilder();
    if(in!=null)try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){
      String s; while((s=r.readLine())!=null)b.append(s);
    }
    if(code>=200&&code<300)cb.ok(code,b.toString());
    else if(code==401){
      AzureAuthManager.clearCachedToken();
      cb.err("AZURE_SIGN_IN_REQUIRED");
    } else cb.err("HTTP "+code+(b.length()==0?"":": "+b));
  }
}