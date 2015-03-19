#include <iostream>

#include "quickStart.h"
#include "developerGuide.hpp"

int main(int argc, char* argv[])
{
	std::cout << "Hello World" << std::endl;
	
	std::cout << "foo() = " << foo() << std::endl;
	
	std::cout << "Bar::Foo() = " << Bar::Foo() << std::endl;

	return EXIT_SUCCESS;
}
