#include <iostream>
#include <GLFW/glfw3.h>
#include <lua.hpp>

int main(int argc, char* argv[])
{
	/* Initialize the library */
	if (!glfwInit())
	{
		return EXIT_FAILURE;
	}

	lua_State* lvm = luaL_newstate();

	lua_pushcfunction(lvm, [](lua_State*lvm) -> int {
		lua_pushinteger(lvm, 640);
		lua_pushinteger(lvm, 480);
		lua_pushstring(lvm, "Hello World!");
		return 3;
	});

	lua_call(lvm, 0, 3);

	auto w = lua_tointeger(lvm, 1);
	auto h = lua_tointeger(lvm, 2);
	auto t = lua_tostring(lvm, 3);

	/* Create a windowed mode window and its OpenGL context */
	GLFWwindow* window = glfwCreateWindow(w, h, t, nullptr, nullptr);
	if (!window)
	{
		glfwTerminate();
		return EXIT_FAILURE;
	}

	/* Make the window's context current */
	glfwMakeContextCurrent(window);

	// close normally when SHIFT + Q is pressed
	glfwSetCharCallback(window, [](GLFWwindow* window, unsigned int keycode)
	{
		if (GLFW_KEY_Q == keycode)
		{
			glfwSetWindowShouldClose(window, GL_TRUE);
		}
	});

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