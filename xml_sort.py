#!/usr/bin/python

import sys
import xml.etree.ElementTree as ElementTree

import lxml.etree as etree

# Avoid "ns0" as a prefix for EPS namespace, add names for rest:
ElementTree.register_namespace('', 'http://www.tolkien7.com/eps/schemas')
ElementTree.register_namespace('SOAP-ENV', 'http://schemas.xmlsoap.org/soap/envelope/')
ElementTree.register_namespace('eb', 'http://www.ebxml.org/namespaces/messageHeader')
ElementTree.register_namespace('wsse', 'http://schemas.xmlsoap.org/ws/2002/12/secext')
ElementTree.register_namespace('OTA', 'http://www.opentravel.org/OTA/2002/11')
ElementTree.register_namespace('wsu', 'http://schemas.xmlsoap.org/ws/2002/12/utility')
ElementTree.register_namespace('xlink', 'http://www.w3.org/1999/xlink')


class XmlFormatter(object):
    """
    Formatter for XML.

    XML is formatted according to two rules:
      1. Child nodes are alphabetically sorted
      2. Attributes are alphabetically sorted
    For #1 there's __sort_child_nodes() method, for attributes ElementTree handles this internally, see
    https://hg.python.org/cpython/file/2.7/Lib/xml/etree/ElementTree.py#l926

    LXML provides white space stripping and pretty-print formatting.
    """

    def __init__(self, sort=True):
        self.__sort = sort

    def output(self, output_stream, content=None, file_name=None):
        output_stream.write(self.format(content=content, file_name=file_name))

    def format(self, content=None, file_name=None):
        if file_name is not None:
            xml = ElementTree.parse(file_name).getroot()
        elif content is not None:
            xml = ElementTree.fromstring(content)
        else:
            raise ValueError('Missing XML file name or content')

        if self.__sort:
            XmlFormatter.__sort_child_nodes(xml)
        return etree.tostring(etree.XML(ElementTree.tostring(xml), parser=etree.XMLParser(remove_blank_text=True)),
            pretty_print=True).strip()

    @staticmethod
    def __sort_child_nodes(element):
        for child in element:
            XmlFormatter.__sort_child_nodes(child)

        sorted_children = sorted(element, key=lambda x: x.items())
        for child in sorted_children:
            element.remove(child)
        for child in reversed(sorted_children):
            element.insert(0, child)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        logging.error('Missing argument: input xml file name')
        sys.exit(-1)

    formatter = XmlFormatter()
    formatter.output(sys.stdout, file_name=sys.argv[1])
