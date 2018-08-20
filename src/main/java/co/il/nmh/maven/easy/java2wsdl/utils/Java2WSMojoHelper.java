package co.il.nmh.maven.easy.java2wsdl.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.maven_plugin.Java2WSMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * @author Maor Hamami
 */

public class Java2WSMojoHelper
{
	public static final Java2WSMojoHelper INSTANCE = new Java2WSMojoHelper();

	private Field classNameField;
	private Field addressField;
	private Field genWsdlField;
	private Field arglineField;
	private Field projectField;
	private Field projectHelperField;
	private Field classpathField;
	private Field classpathElementsField;
	private List<Field> booleanFields;

	private Java2WSMojoHelper()
	{
		booleanFields = new ArrayList<Field>();

		try
		{
			classNameField = Java2WSMojo.class.getDeclaredField("className");
			classNameField.setAccessible(true);

			addressField = Java2WSMojo.class.getDeclaredField("address");
			addressField.setAccessible(true);

			genWsdlField = Java2WSMojo.class.getDeclaredField("genWsdl");
			genWsdlField.setAccessible(true);

			arglineField = Java2WSMojo.class.getDeclaredField("argline");
			arglineField.setAccessible(true);

			projectField = Java2WSMojo.class.getDeclaredField("project");
			projectField.setAccessible(true);

			projectHelperField = Java2WSMojo.class.getDeclaredField("projectHelper");
			projectHelperField.setAccessible(true);

			classpathField = Java2WSMojo.class.getDeclaredField("classpath");
			classpathField.setAccessible(true);

			classpathElementsField = Java2WSMojo.class.getDeclaredField("classpathElements");
			classpathElementsField.setAccessible(true);

			Field[] declaredFields = Java2WSMojo.class.getDeclaredFields();

			for (Field field : declaredFields)
			{
				if (field.getType() == Boolean.class)
				{
					field.setAccessible(true);
					booleanFields.add(field);
				}
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException("failed to investigate Java2WSMojo.class");
		}
	}

	public Java2WSMojo createJava2WSMojo(Class<?> webservice, String address, MavenProject project, MavenProjectHelper projectHelper) throws MojoExecutionException
	{
		try
		{
			Java2WSMojo java2wsMojo = new Java2WSMojo();

			for (Field field : booleanFields)
			{
				field.set(java2wsMojo, Boolean.FALSE);
			}

			classNameField.set(java2wsMojo, webservice.getName());
			addressField.set(java2wsMojo, address + webservice.getSimpleName());
			genWsdlField.set(java2wsMojo, Boolean.TRUE);
			arglineField.set(java2wsMojo, "-createxsdimports");
			projectField.set(java2wsMojo, project);
			projectHelperField.set(java2wsMojo, projectHelper);
			classpathField.set(java2wsMojo, "");
			classpathElementsField.set(java2wsMojo, project.getCompileClasspathElements());

			return java2wsMojo;
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("failed to create Java2WSMojo for " + webservice.getName());
		}
	}
}
