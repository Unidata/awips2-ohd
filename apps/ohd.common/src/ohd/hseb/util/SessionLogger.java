package ohd.hseb.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;


public class SessionLogger implements Logger
{
	private String _currentFileName = null;
	private OutputStream _outputStream = null;
	private PrintWriter _writer = null;
	private boolean _usingRealFile = false;
	private boolean _fileIsOpen = false;
	private int _sessionId;

	private static final String dateFormatString =
		"yyyyMMdd";
	private static final SimpleDateFormat _dateFormatter = 
		new SimpleDateFormat(dateFormatString);

	private static final String timeFormatString =
		"HH:mm:ss";
	private static final SimpleDateFormat _timeFormatter = 
		new SimpleDateFormat(timeFormatString);

	private boolean _appendDateTime = false;
	private boolean _keepFileOpen = true;
	private String _messageId;
	private String _baseFileName;
    private String _appName;

	static
	{
		_dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));    
		_timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}        

	public SessionLogger(String appName, String baseFileName,
                        boolean keepFileOpen, boolean appendDateTime, String messageId)
	{
		_keepFileOpen = true;
		_appendDateTime = true;
		_messageId = messageId;
		_baseFileName = baseFileName;
        _appName = appName;
	} 

	public int getSessionId()
	{
		return _sessionId;	
	}

	public void log(String message)
	{	
		boolean isCreatedNow = false;
		if (!isOpen() || message == null)
		{
			openFile(_baseFileName, false);
			isCreatedNow = true;
			_writer = new PrintWriter(_outputStream);
		}
		else
		{
			String splitStr[] = _currentFileName.split("\\.");
			String dateTimeStampInFileName = splitStr[2];
			if(!(getCurrentDate().equalsIgnoreCase(dateTimeStampInFileName)))
			{
				openFile(_currentFileName, true);
				isCreatedNow = true;
				_writer = new PrintWriter(_outputStream);
			}
		}

		if(isCreatedNow)
		{
			if(_messageId == null)
				_messageId = new Integer(_sessionId).toString();
			_writer.println(_messageId);	
			_writer.println("**************************************");
		}

		if (_appendDateTime)
		{
			if(message != null)
				_writer.println( getCurrentTime() + ": " + message);
		}
		else
		{
			if(message != null)
				_writer.println(message);
		}
		_writer.flush();
		if (!_keepFileOpen)
		{
			close();    
		}
	}

	private void determineSessionId(String fileName)
	{
		int sessionId = -1;
		String dirName;
		int index = fileName.lastIndexOf("/");
		dirName = fileName.substring(0, index);
		File dir = new File(dirName);
		int leastPossibleNumber = 1;
		if(dir.exists())
		{
			if(dir.isDirectory())
			{
				String files[] = dir.list();
				if(files != null)
				{
					if(files.length == 0)
					{
						sessionId = 1;
					}
					else
					{
						int existingIds[] = new int[files.length];
						for(int i=0; i < files.length; i++)
						{
							String str = files[i];
							String splitStr[] = str.split("\\.");
							if(splitStr == null)
								continue;
							else if(splitStr.length != 3)
								continue;
							else if(!(splitStr[0].compareTo(_appName) == 0))
								continue;
							else if(splitStr[2].length() != 8)
								continue;
							try
							{
								existingIds[i] = Integer.parseInt(splitStr[1]);
								Integer.parseInt(splitStr[2]);// To make sure the last portion is a data (number)
							}
							catch(Exception e)
							{
								continue;
							}
						}
						Arrays.sort(existingIds);
						while(true)
						{
							if(Arrays.binarySearch(existingIds, leastPossibleNumber) < 0)
							{
								sessionId = leastPossibleNumber;
								break;
							}
							else
								leastPossibleNumber++;
						}
					}
				} 
			}
			else
			{
				System.out.println(_appName + ":["+dir+"] isn't a directory");
			}
		}
		else
		{
			System.out.println(_appName + ":Directory ["+dir+"] doesn't exist");
		}

		_sessionId = sessionId;
	}

	public PrintWriter getPrintWriter()
	{
		return _writer;
	}

	private String getCurrentDate()
	{
		Date date = new Date();
		return _dateFormatter.format(date);	
	}

	private String getCurrentTime()
	{
		Date date = new Date();
		return _timeFormatter.format(date);	
	}

	private void openFile(String fileName, boolean isExistingSession)
	{
		try
		{
			if (fileName != null)
			{
				if(!isExistingSession)
				{
					determineSessionId(_baseFileName);
				}
				if(_sessionId != -1) // !=-1 means able to get a session id that is valid
				{
					_currentFileName = _baseFileName +"."+ _sessionId + "."+getCurrentDate();
					_outputStream = new FileOutputStream(_currentFileName, true);
					_usingRealFile = true;
					_fileIsOpen = true;
				}
				else // unable to get a session id so log to console
				{
					_outputStream = System.out;
				}
			}
			else //fileName == null
			{
				_outputStream = System.out;
			}
			_writer = new PrintWriter(_outputStream);
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}
	}

	public void close() 
	{
		if (isOpen() && _usingRealFile)
		{
			_fileIsOpen = false;
			_writer.close();
		}
	}

	private boolean isOpen()
	{
		return _fileIsOpen;	
	}
}
