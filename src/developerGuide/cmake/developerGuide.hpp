#pragma once

class Bar
{
	Bar(const Bar&) = delete;
	Bar& operator= (const Bar&) = delete;
public:
	static float Foo(void);
};
