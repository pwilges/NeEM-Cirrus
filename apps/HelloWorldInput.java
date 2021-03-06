package apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;

public class HelloWorldInput extends Thread{
	
	//Atributos
	private MulticastChannel neem;
	private int numMsgEnv; //para diferenciar uma mensagem enviada da outra
	private int numMsgRec; //contador do número de mensagens recebidas
	
	//Construtor
	public HelloWorldInput(MulticastChannel mult_channel)
	{
		this.neem = mult_channel;
		this.numMsgEnv = 0;
		this.numMsgRec = 0;
		this.neem.setLoopbackMode(false);	//se está em loopback mode, supostamente a mensagem enviada pelo ponto não deve ser recebida por ele mesmo
		setDaemon(true);
        start();
	}
	
	//Métodos
	
	public void enviaParaNuvem() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Aperte qualquer coisa para iniciar uma rodada de gossiping");
		while (br.readLine() != null) 
		{
			this.numMsgEnv++;
			String msg = new String("MsgID=" + this.numMsgEnv + "| Hello World " + this.numMsgEnv + " de " + neem.getLocalSocketAddress());
			ByteBuffer mensagem = ByteBuffer.wrap(msg.getBytes());
			neem.write(mensagem);
		}
		
	}
	
	public void run() //recebe da nuvem
	{
		try
		{
			while (true)
			{
				byte[] b = new byte[500]; 
				ByteBuffer bb = ByteBuffer.wrap(b);
				neem.read(bb);
				this.numMsgRec++;
				System.out.println(this.numMsgRec + " Msg recebida: \"" + new String(b));
			} 
		} catch (AsynchronousCloseException ace) {
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) //se não possuir argumentos, exibe mensagem na tela e encerra
		{
			System.err.println("Modo de uso: apps.HelloWorld endereço_local endereço_ponto_1 ... endereço_ponto_n");
			System.exit(1);
		}
		
		try
		{
			MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true)); //instancia o NeEM com a classe Multicast Channel
			
			//registra o bean para o JMX. O acesso se dá pelo jconsole em /usr/java/sdk*/bin
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ProtocolMBean mbean = neem.getProtocolMBean();
            ObjectName name = new ObjectName("net.sf.neem:type=Protocol,id="+mbean.getLocalId());
            mbs.registerMBean(mbean, name);
			
			HelloWorldInput hello = new HelloWorldInput(neem); //instancia essa classe com o NeEM
			
			System.out.println("Executando no endereço " + neem.getLocalSocketAddress());
			
			for(int i=1; i<args.length; i++)
			{
				neem.connect(Addresses.parse(args[i], false));//conecta aos outros pontos informados
			}
			
			hello.enviaParaNuvem(); //envia mensagem para a nuvem
			
			neem.close(); //fecha o canal
			
		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}

}
