package base;

import java.net.Socket;
import java.util.Scanner;

import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;

public class ConsoleThread extends Thread{
	ClientServerNode server;
	public boolean runConsole = false;
	public Scanner a;

	public ConsoleThread(ClientServerNode sn, Scanner sa) {
		server = sn;
		a = sa;
	}

	/**
	 * Prints prompt and take input from console
	 */
	public void run(){
		runConsole = true;
		while(true)
		{
			System.out.print("Please Enter the Test/Unit/Command you want to run (Enter X to exit)\n");
			System.out.print("Enter parameters separated by a space (Enter C for commands)\n");
			String input = a.nextLine();
			TestInterface(input);

		}
	}
	/**
	 * @throws Exception
	 * Calls on proper unit/test based on console input
	 */
	protected void TestInterface(String input){

		String delim = "[ ]+";
		String[] tokens = input.split(delim);
		try {
			switch (tokens[0]) {
			case ("Unit1"):
				if (tokens.length == 3)
					server.unit1(Integer.parseInt(tokens[1]),Integer.parseInt(tokens[2]));
				else
					throw new Exception();
			break;
			case ("Test1"):
				if (tokens.length == 2)
					server.test1(Integer.parseInt(tokens[1]));
				else
					throw new Exception();
			break;
			case ("Test2"):
			case ("Unit2"):
				if (tokens.length == 3) {
					server.test2(tokens[1], Integer.parseInt(tokens[2]));
				} else
					throw new Exception();
			break;
			case ("Test3"):
			case ("Unit3"):
				if (tokens.length == 2)
					server.test3(tokens[1]);
				else
					throw new Exception();
			break;
			case ("Unit4"):
				if (tokens.length == 4){
					server.unit4(tokens[1].toString(), tokens[2].toString(), Integer.parseInt(tokens[3]));
				}
				else{
					throw new Exception();
				}
			break;
			case ("Test4"):
				if (tokens.length == 3){
					server.test4(tokens[1].toString(), tokens[2].toString());
				}
				else{
					throw new Exception();
				}
			break;
			case ("Test5"):
			case ("Unit5"):
				if (tokens.length == 3)
					server.test5(tokens[1].toString(), tokens[2].toString());
				else
					throw new Exception();
			break;
			case ("Test6"):
				if (tokens.length == 3)
					server.test6(tokens[1].toString(), tokens[2].toString());
				else
					throw new Exception();
			break;
			case ("Unit6"):
				if (tokens.length == 3)
					server.unit6(tokens[1].toString(), tokens[2].toString());
				else
					throw new Exception();
			break;
			case ("Test7"):
			case("Unit7"):
				if (tokens.length == 2)
					server.test7(tokens[1].toString());
				else
					throw new Exception();
			break;
			case ("Unit8"): 
				System.out.println("Input Test6 on mutiple clients to run unit 8");
			break;
			case ("X"):
			case ("x"):
				System.exit(0);
			break;
			case ("C"): 
				server.printCommands();
			break;
			default:
				throw new Exception();
			}
		} catch (Exception e) {

			//e.printStackTrace();
			System.out.println("Unable to Complete Request\n");
		}


	}

}
