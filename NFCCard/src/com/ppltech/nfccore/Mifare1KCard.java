package com.ppltech.nfccore;


public class Mifare1KCard implements INFCCard {
	
	public final static int BLOCK_SIZE = 16;

	// All variables that requires pass-by-reference calls to functions are
	// declared as 'Array of int' with length 1
	// Java does not process pass-by-ref to int-type variables, thus Array of
	// int was used.
	int[] ATRLen = new int[1];
	int[] hContext = new int[1];
	int[] cchReaders = new int[1];
	int[] hCard = new int[1];
	int[] PrefProtocols = new int[1];
	int[] RecvLen = new int[1];
	int SendLen = 0;
	int[] nBytesRet = new int[1];
	byte[] SendBuff = new byte[262];
	byte[] RecvBuff = new byte[262];
	byte[] szReaders = new byte[1024];
	int reqType;
	ACSModule.SCARD_READERSTATE rdrState = new ACSModule.SCARD_READERSTATE();

	String rdrcon = "";

	static JacspcscLoader jacs = new JacspcscLoader();

	@Override
	public byte[] read() {
		
		String tmpStr = "", tmpHex = "";

		connect();
		// Block 1 to 64 (4 Sectors - 16 Blocks each)
		// 0 - 3 Blocks are header information
		
		for (int blk = 4; blk < 64; blk++) {
			
			//Leave the last frame in every sector
			if ((blk % 4 != 3) && authenticate(blk)) {
				
				clearBuffers();
				//read binary block command
				SendBuff[0] = (byte)0xFF;
				SendBuff[1] = (byte)0xB0;
				SendBuff[2] = (byte)0x00;
				SendBuff[3] = (byte)blk;//Blk number
				SendBuff[4] = (byte)BLOCK_SIZE; //Blk size
				
				SendLen = 5;
				RecvLen[0] = BLOCK_SIZE + 2;
				
				int retCode = sendAPDUandDisplay();
				if(retCode != ACSModule.SCARD_S_SUCCESS)
					return null;
				else
				{
					
					for(int i =RecvLen[0]-2; i<RecvLen[0]; i++)
					{
						
						tmpHex = Integer.toHexString(((Byte)RecvBuff[i]).intValue() & 0xFF).toUpperCase();
						
						//For single character hex
						if (tmpHex.length() == 1) 
							tmpHex = "0" + tmpHex;
						
						tmpStr += " " + tmpHex; 
						
					}
					
					//check for response
					if(tmpStr.trim().equals("90 00"))
					{
						byte[] bytes = new byte[RecvLen[0]-1];
						
						tmpStr = "";
						for(int i =0; i<RecvLen[0]-1; i++) {
							bytes[i] = RecvBuff[i];
							if(RecvBuff[i] <= 0)
								tmpStr = tmpStr + " ";
							else
								tmpStr = tmpStr + (char)RecvBuff[i];
						}
//						PatientEMR pat = (PatientEMR) Convertor.toObject(bytes, PatientEMR.class);
//						tBinData.setText(pat.PMH.toString());
						
					}
					else
						return null;
					
				}
				
			}
			else {
				
			}
		}

		return null;
	}

	@Override
	public boolean write(byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean initializeDevice() {
		// 1. Establish context and obtain hContext handle
		int retCode = jacs.jSCardEstablishContext(ACSModule.SCARD_SCOPE_USER,
				0, 0, hContext);

		if (retCode != ACSModule.SCARD_S_SUCCESS) {
			// Problem in initializing
			return false;
		}

		// 2. List PC/SC card readers installed in the system
		retCode = jacs.jSCardListReaders(hContext, 0, szReaders, cchReaders);

		if (retCode != ACSModule.SCARD_S_SUCCESS) {
			// Problem in initializing
			return false;
		} else {
			int offset = 0;

			for (int i = 0; i < cchReaders[0] - 1; i++) {

				if (szReaders[i] == 0x00) {
					rdrcon = new String(szReaders, offset, i - offset);
					offset = i + 1;
				}
			}
		}

		return true;
	}

	@Override
	public boolean connect() {
		int retCode;
		Boolean connActive = initializeDevice();

		if (connActive) {
			retCode = jacs
					.jSCardDisconnect(hCard, ACSModule.SCARD_UNPOWER_CARD);
		}

		retCode = jacs.jSCardConnect(hContext, rdrcon,
				ACSModule.SCARD_SHARE_SHARED, ACSModule.SCARD_PROTOCOL_T1
						| ACSModule.SCARD_PROTOCOL_T0, hCard, PrefProtocols);

		if (retCode != ACSModule.SCARD_S_SUCCESS) {
			// displayOut(1, retCode, "");
			connActive = true;
			return false;

		} else {

			// displayOut(0, 0, "Successful connection to " +
			// (String)cbReader.getSelectedItem());

		}

		connActive = true;

		return true;
	}

	private Boolean authenticate(int block) {
		
		String tmpStr = "", tmpHex = "";

		clearBuffers();
		// authentication command
		SendBuff[0] = (byte) 0xFF;
		SendBuff[1] = (byte) 0x86;
		SendBuff[2] = (byte) 0x00;
		SendBuff[3] = (byte) 0x00;
		SendBuff[4] = (byte) 0x05;
		SendBuff[5] = (byte) 0x01;
		SendBuff[6] = (byte) 0x00;
		SendBuff[7] = (byte) (block);

		// if(rbKeyA.isSelected())
		SendBuff[8] = (byte) 0x60;
		SendBuff[9] = (byte) 0;

		SendLen = 10;
		RecvLen[0] = 2;

		int retCode = sendAPDUandDisplay();

		if (retCode != ACSModule.SCARD_S_SUCCESS)
			return false;
		else {

			for (int i = RecvLen[0] - 2; i < RecvLen[0]; i++) {

				tmpHex = Integer.toHexString(
						((Byte) RecvBuff[i]).intValue() & 0xFF).toUpperCase();

				// For single character hex
				if (tmpHex.length() == 1)
					tmpHex = "0" + tmpHex;

				tmpStr += " " + tmpHex;

			}
			// check for response
			if (tmpStr.trim().equals("90 00"))
				// displayOut(2, 0, "Authentication Success");
				return true;
			else
				// displayOut(1, 0, "Authentication Failed");
				return false;

		}

	}

	public void clearBuffers() {

		for (int i = 0; i < 262; i++) {

			SendBuff[i] = (byte) 0x00;
			RecvBuff[i] = (byte) 0x00;

		}

	}

	public int sendAPDUandDisplay() {
		int retCode;
		ACSModule.SCARD_IO_REQUEST IO_REQ = new ACSModule.SCARD_IO_REQUEST();
		ACSModule.SCARD_IO_REQUEST IO_REQ_Recv = new ACSModule.SCARD_IO_REQUEST();
		IO_REQ.dwProtocol = PrefProtocols[0];
		IO_REQ.cbPciLength = 8;
		IO_REQ_Recv.dwProtocol = PrefProtocols[0];
		IO_REQ_Recv.cbPciLength = 8;

		String tmpStr = "", tmpHex = "";

		for (int i = 0; i < SendLen; i++) {

			tmpHex = Integer
					.toHexString(((Byte) SendBuff[i]).intValue() & 0xFF)
					.toUpperCase();
			// JOptionPane.showMessageDialog(this, SendBuff[4]);
			// For single character hex
			if (tmpHex.length() == 1)
				tmpHex = "0" + tmpHex;

			tmpStr += " " + tmpHex;

		}

		retCode = jacs.jSCardTransmit(hCard, IO_REQ, SendBuff, SendLen, null,
				RecvBuff, RecvLen);

		if (retCode != ACSModule.SCARD_S_SUCCESS) {
			return retCode;
		}

		tmpStr = "";

		for (int i = 0; i < RecvLen[0]; i++) {

			tmpHex = Integer
					.toHexString(((Byte) RecvBuff[i]).intValue() & 0xFF)
					.toUpperCase();

			// For single character hex
			if (tmpHex.length() == 1)
				tmpHex = "0" + tmpHex;

			tmpStr += " " + tmpHex;

		}

		return retCode;
	}

}
