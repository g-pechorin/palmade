#include <iostream>
#include <GLFW/glfw3.h>

int main(int argc, char* argv[])
{
	std::cout << "Hello!" << std::endl;

	GLFWwindow* window;

	/* Initialize the library */
	if (!glfwInit())
	{
		return EXIT_FAILURE;
	}

	/* Create a windowed mode window and its OpenGL context */
	window = glfwCreateWindow(640, 480, "Hello World", NULL, NULL);
	if (!window)
	{
		glfwTerminate();
		return EXIT_FAILURE;
	}

	/* Make the window's context current */
	glfwMakeContextCurrent(window);

	/* Loop until the user closes the window */
	while (!glfwWindowShouldClose(window))
	{
		/* Render here */

		/* Swap front and back buffers */
		glfwSwapBuffers(window);

		/* Poll for and process events */
		glfwPollEvents();
	}

	glfwTerminate();
	return EXIT_SUCCESS;
}