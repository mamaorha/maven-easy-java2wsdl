package co.il.nmh.maven.easy.java2wsdl.data;

/**
 * @author Maor Hamami
 */
public class WsdlInfo
{
	private String wsdlPath;
	private String service;

	public String getService()
	{
		return this.service;
	}

	public void setService(String service)
	{
		this.service = service;
	}

	public String getWsdl()
	{
		return this.wsdlPath;
	}

	public void setWsdl(String wsdl)
	{
		this.wsdlPath = wsdl;
	}

	@Override
	public String toString()
	{
		return "WsdlInfo [wsdlPath=" + wsdlPath + ", service=" + service + "]";
	}
}
