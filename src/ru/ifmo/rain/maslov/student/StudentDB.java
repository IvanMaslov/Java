package ru.ifmo.rain.maslov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

public class StudentDB implements AdvancedStudentGroupQuery {
    private final static Comparator<Student> STUDENT_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .thenComparing(Student::getId);

    private final static Comparator<Group> GROUP_COMPARATOR =
            Comparator.comparing(Group::getName);

    private static String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private static Stream<String> getSomeStream(List<Student> students, Function<Student, String> getter) {
        return students.stream()
                .map(getter);
    }

    private static List<String> getSome(List<Student> students, Function<Student, String> getter) {
        return getSomeStream(students, getter)
                .collect(Collectors.toList());
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> comp) {
        return students.stream()
                .sorted(comp)
                .collect(Collectors.toList());
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> pred) {
        return students.stream()
                .filter(pred)
                .sorted(STUDENT_COMPARATOR)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getSome(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getSome(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getSome(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getSome(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getSomeStream(students, Student::getFirstName)
                .collect(Collectors.toSet());
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, STUDENT_COMPARATOR);
    }

    private static Predicate<Student> checkProperty(Function<Student, String> property, String expectedValue) {
        return (student -> property.apply(student).equals(expectedValue));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, checkProperty(Student::getFirstName, name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, checkProperty(Student::getLastName, name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, checkProperty(Student::getGroup, group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(x -> group.equals(x.getGroup()))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }

    private List<Group> getGroupBy(Collection<Student> students, Comparator<Student> comp) {
        return students.stream()
                .sorted(comp)
                .collect(Collectors.groupingBy(
                        Student::getGroup,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(x -> new Group(x.getKey(), x.getValue()))
                .sorted(GROUP_COMPARATOR)
                .collect(Collectors.toList());

    }

    private String getLargestGroupBy(Collection<Student> students, Comparator<Group> comp) {
        return getGroupsByName(students).stream()
                .max(comp)
                .map(Group::getName)
                .orElse("");
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, STUDENT_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, Comparator.comparing(Student::getId));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingInt(a -> a.getStudents().size()));
    }

    // long comp
    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingLong(a ->
                a.getStudents()
                        .stream()
                        .map(Student::getFirstName)
                        .distinct()
                        .count()));
    }

    private List<String> getStudentByIndices(List<String> students, int[] indices) {
        return Arrays.stream(indices)
                .boxed()
                .map(students::get)
                .collect(Collectors.toList());
    }

    private List<String> idMap(Collection<Student> students, Function<Student, String> property) {
        return students.stream()
                .map(property)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Integer> nameMap(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(StudentDB::getFullName,
                        Collectors.mapping(Student::getGroup,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))));
    }

    private String getPriorityStudent(Collection<Student> students, Map<String, Integer> priority) {
        return students.stream()
                .max(Comparator.comparing(
                        (Student x) -> priority.getOrDefault(getFullName(x), -1))
                        .thenComparing(StudentDB::getFullName))
                .map(StudentDB::getFullName)
                .orElse("");
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getPriorityStudent(students, nameMap(students));
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getStudentByIndices(idMap(students, Student::getFirstName), indices);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getStudentByIndices(idMap(students, Student::getLastName), indices);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getStudentByIndices(idMap(students, Student::getGroup), indices);
    }

    //fix not for all
    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getStudentByIndices(idMap(students, StudentDB::getFullName), indices);
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.student AdvancedStudentGroupQuery  ru.ifmo.rain.maslov.student.StudentDB hello
