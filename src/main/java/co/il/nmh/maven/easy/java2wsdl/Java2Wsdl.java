package co.il.nmh.maven.easy.java2wsdl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jws.WebService;

import org.apache.cxf.maven_plugin.Java2WSMojo;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import co.il.nmh.easy.maven.utils.MavenGenericUtils;
import co.il.nmh.easy.utils.FileUtils;
import co.il.nmh.maven.easy.java2wsdl.data.WsdlInfo;
import co.il.nmh.maven.easy.java2wsdl.merge.WsdlMerger;
import co.il.nmh.maven.easy.java2wsdl.utils.Java2WSMojoHelper;

/**
 * @author Maor Hamami
 */
@Mojo(name = "java2wsdl", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class Java2Wsdl extends AbstractMojo
{
	@Component
	private MavenProject project;

	@Component
	private MavenProjectHelper projectHelper;

	@Component
	private PluginDescriptor descriptor;

	@Parameter(property = "wsdl.servicePackages", required = false)
	private List<String> servicePackages;

	@Parameter(property = "wsdl.services", required = false)
	private List<String> services;

	@Parameter(property = "wsdl.address", required = true)
	private String address;

	@Parameter(property = "wsdl.wsdlUrl", required = true)
	private String wsdlUrl;

	@Parameter(property = "wsdl.generatedWsdlPath", required = true)
	private String generatedWsdlPath;

	@Parameter(property = "wsdl.generatedWsdlName", required = true)
	private String generatedWsdlName;

	@Parameter(property = "wsdl.generatedWsdlService", required = true)
	private String generatedWsdlService;

	public void execute() throws MojoExecutionException, MojoFailureException
	{
		adjustUrls();
		MavenGenericUtils.adjustClassLoader(project, descriptor);

		Set<Class<?>> webservices = getWebservices(servicePackages, new HashSet<String>(services));

		createWsdls(webservices);
		mergeWsdls(webservices);
	}

	private void adjustUrls()
	{
		if (!address.endsWith("/"))
		{
			address += "/";
		}

		if (!generatedWsdlPath.endsWith("/"))
		{
			generatedWsdlPath += "/";
		}
	}

	private Set<Class<?>> getWebservices(List<String> packages, Set<String> classes)
	{
		ConfigurationBuilder reflectionConfiguration = new ConfigurationBuilder();
		reflectionConfiguration.addUrls(getURLS(packages, classes));
		reflectionConfiguration.addScanners(new MethodAnnotationsScanner());

		Reflections reflections = new Reflections(reflectionConfiguration);
		Set<Class<?>> webservices = reflections.getTypesAnnotatedWith(WebService.class);

		Set<Class<?>> relevantWebservices = new HashSet<Class<?>>();

		for (Class<?> webservice : webservices)
		{
			String name = webservice.getName();

			if (classes.contains(name))
			{
				relevantWebservices.add(webservice);
				continue;
			}

			for (String currPackage : packages)
			{
				if (name.startsWith(currPackage))
				{
					relevantWebservices.add(webservice);
					break;
				}
			}
		}

		return relevantWebservices;
	}

	private Set<URL> getURLS(List<String> packages, Set<String> classes)
	{
		Set<String> existPackages = new HashSet<String>();
		Set<URL> urls = new HashSet<URL>();

		for (String clazz : classes)
		{
			int lastDot = clazz.lastIndexOf(".");

			if (lastDot > -1)
			{
				clazz = clazz.substring(0, lastDot);
			}

			if (!existPackages.contains(clazz))
			{
				Collection<URL> currUrls = ClasspathHelper.forPackage(clazz);
				urls.addAll(currUrls);

				existPackages.add(clazz);
			}
		}

		for (String currPackage : packages)
		{
			if (!existPackages.contains(currPackage))
			{
				Collection<URL> currUrls = ClasspathHelper.forPackage(currPackage);
				urls.addAll(currUrls);

				existPackages.add(currPackage);
			}
		}

		return urls;
	}

	private void createWsdls(Set<Class<?>> webservices) throws MojoExecutionException
	{
		for (Class<?> webservice : webservices)
		{
			try
			{
				Java2WSMojo java2wsMojo = Java2WSMojoHelper.INSTANCE.createJava2WSMojo(webservice, address, project, projectHelper);
				java2wsMojo.execute();
			}
			catch (Exception e)
			{
				throw new MojoExecutionException("failed to run Java2WSMojo on " + webservice.getName());
			}
		}
	}

	private void mergeWsdls(Set<Class<?>> webservices) throws MojoExecutionException
	{
		if (!webservices.isEmpty())
		{
			if (!FileUtils.createDir(generatedWsdlPath))
			{
				throw new MojoExecutionException("Failed to create dir at " + generatedWsdlPath);
			}

			File wsdl = new File(generatedWsdlPath + generatedWsdlName + ".wsdl");
			List<WsdlInfo> wsdlsInfo = new ArrayList<WsdlInfo>();

			for (Class<?> webservice : webservices)
			{
				WsdlInfo wsdlInfo = new WsdlInfo();
				wsdlInfo.setWsdl(generatedWsdlPath + webservice.getSimpleName() + ".wsdl");
				wsdlInfo.setService(webservice.getSimpleName());

				wsdlsInfo.add(wsdlInfo);
			}

			WsdlMerger wsdlMerger = new WsdlMerger(getLog(), wsdlUrl, wsdl, generatedWsdlService, wsdlsInfo);
			wsdlMerger.merge();
		}
	}
}
